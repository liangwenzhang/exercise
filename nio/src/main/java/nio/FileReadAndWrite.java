package nio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FileReadAndWrite {

    //分片
    public static void slice() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(10);
        for (int i = 0; i < byteBuffer.capacity(); i++) {
            byteBuffer.put((byte) i);
        }
        byteBuffer.position(0);
        while (byteBuffer.remaining() > 0) {
            System.out.println(byteBuffer.get());
        }
        byteBuffer.position(5);
        byteBuffer.limit(6);
        ByteBuffer slice = byteBuffer.slice();
        while (slice.hasRemaining()) {
            System.out.println(slice.get());
        }
        slice.put(0, (byte) (slice.get(0) * 5));
        slice.position(0);
        while (slice.hasRemaining()) {
            System.out.println(slice.get());
        }
    }

    public static void readFileOfNio(String filePath) throws IOException {
        try (
                FileInputStream inputStream = new FileInputStream(filePath);
                FileChannel inFileChannel = inputStream.getChannel();
        ) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(3);
            int i = 0;
            while (true) {
                System.out.println(i++);
                int read = inFileChannel.read(byteBuffer);
                if (read == -1) {
                    break;
                }
                System.out.println(new String(byteBuffer.array(), StandardCharsets.UTF_8));
                byteBuffer.clear();
            }
        }
    }

    public static void readWriteFile(String sourcePath, String targetPath) throws IOException {
        try (
                FileInputStream inputStream = new FileInputStream(sourcePath);
                FileOutputStream outputStream = new FileOutputStream(targetPath);
                FileChannel inFileChannel = inputStream.getChannel();
                FileChannel outFileChannel = outputStream.getChannel();
        ) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(3);
            while (true) {
                byteBuffer.clear();
                int read = inFileChannel.read(byteBuffer);
                if (read == -1) {
                    break;
                }
                byteBuffer.flip();
                outFileChannel.write(byteBuffer);
            }
        }

    }

    public static void main(String[] args) throws IOException {
//        slice();
//        readFileOfNio("H:\\git projects\\exercise\\nio\\src\\main\\resources\\test");
        readWriteFile("H:\\git projects\\exercise\\nio\\src\\main\\resources\\test", "H:\\git projects\\exercise\\nio\\src\\main\\resources\\target");
    }
}
