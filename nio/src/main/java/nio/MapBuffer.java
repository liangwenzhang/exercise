package nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 *  copy on write 写时 复制新页， 不改变原始文件分页数据
 */
public class MapBuffer {
    public static void main(String[] args) throws IOException {
        File tempFile = File.createTempFile("temp",null);
        FileChannel channel = new RandomAccessFile(tempFile,"rw").getChannel();

        ByteBuffer temp = ByteBuffer.allocate(100);
        temp.put("This is the file content".getBytes());
        temp.flip();
        channel.write(temp,0);
        temp.clear();
        temp.put("This is more file content".getBytes());
        temp.flip();
        channel.write(temp, 8192);

        /**
         * 三种缓冲区
         */
        MappedByteBuffer ro = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        MappedByteBuffer rw = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
        MappedByteBuffer cow = channel.map(FileChannel.MapMode.PRIVATE, 0, channel.size());

        System.out.println("begin:");
        showByteBuffer(ro,rw,cow);

        /**
         * 改变及时缓冲区
         */
        cow.position(8);
        cow.put("COW".getBytes());
        System.out.println("Change to COW buffer");
        showByteBuffer(ro, rw, cow);
/**
 * Change to COW  buffer
 * R/O: 'This is t R/W le content|[8168 nulls]|Th R/W  more file content'
 * R/W: 'This is t R/W le content|[8168 nulls]|Th R/W  more file content'
 * COW: 'This is COW file content|[8168 nulls]|Th R/W  more file content'
 * 下面测试改动文件内容后，3个通道是否对改动可见。
 * 结论：R/O、R/W可见，但是COW缓冲区中有更改页的部分对改动不可见，COW缓冲区中没有更改的内存页对文件的更改是可见的
 */
        rw.position(9);
        rw.put("R/W".getBytes());
        rw.position(8194);
        rw.put(" R/W ".getBytes());
        rw.force();
        System.out.println("Change to R/W buffer");
        showByteBuffer(ro, rw, cow);

        /**
         * Write on channel
         * R/O: 'Channel write le content|[8168 nulls]|Th R/W  moChannel write t'
         * R/W: 'Channel write le content|[8168 nulls]|Th R/W  moChannel write t'
         * COW: 'This is COW file content|[8168 nulls]|Th R/W  moChannel write t'
         * 下面测试改动文件内容，3个通道是否对改动可见
         * 结论：R/O、R/W可见，COW虽然也是可见的，但是是因为文件的改动是COW缓冲区中没有改动过的内存页。
         */
        temp.clear();
        temp.put("Channel write ".getBytes());
        temp.flip();
        channel.write(temp, 0);
        /**
         * 这里解释下rewind和flip间的区别，他们都是将缓冲区转换成可读取状态，但是flip会修改limit，rewind不会
         */
        temp.rewind();
        channel.write(temp, 8202);
        System.out.println("Write on channel");
        showByteBuffer(ro, rw, cow);
        /**
         * Second change to COW buffer
         * R/O: 'Channel write le content|[8168 nulls]|Th R/W  moChannel write t'
         * R/W: 'Channel write le content|[8168 nulls]|Th R/W  moChannel write t'
         * COW: 'This is COW file content|[8168 nulls]|Th R/W  moChann COW2 te t'
         * 下面测试修改即时缓冲区内容，其他缓冲区是否可见
         * 结论：对即时缓冲内容进行修改时，其他缓冲区不可见
         */
        cow.position(8207);
        cow.put(" COW2 ".getBytes());
        System.out.println("Second change to COW buffer");
        showByteBuffer(ro, rw, cow);
        /**
         * Second change to R/W buffer
         * R/O: ' R/W2 l write le content|[8168 nulls]|Th R/W  moChannel  R/W2 t'
         * R/W: ' R/W2 l write le content|[8168 nulls]|Th R/W  moChannel  R/W2 t'
         * COW: 'This is COW file content|[8168 nulls]|Th R/W  moChann COW2 te t'
         * 下面测试修改文件内容，3个通道是否可见
         * 结论：修改文件内容后，由于修改的是COW缓冲区中有改动的内存页，所以对即时缓冲区不可见，对其他缓冲区可见
         */
        rw.position(0);
        rw.put(" R/W2 ".getBytes());
        rw.position(8210);
        rw.put(" R/W2 ".getBytes());
        rw.force();
        System.out.println("Second change to R/W buffer");
        showByteBuffer(ro, rw, cow);
        channel.close();
        tempFile.delete();


    }
    public static void showByteBuffer(ByteBuffer ro ,ByteBuffer rw,ByteBuffer cow){
        dumpBuffer("R/O", ro);
        dumpBuffer("R/W", rw);
        dumpBuffer("COW", cow);
        System.out.println("");
    }
    public static void dumpBuffer(String prefix,ByteBuffer byteBuffer){
        System.out.println(prefix+":");
        int nulls = 0;
        int limit = byteBuffer.limit();
        for (int i = 0; i < limit; i++) {
            char c = (char) byteBuffer.get(i);
            if(c == '\u0000'){
                nulls ++;
                continue;
            }
            if(nulls !=0){
                System.out.print("[blank:"+nulls+"]");
                nulls = 0;
            }
            System.out.print(c);
        }
        System.out.println();
    }
}
