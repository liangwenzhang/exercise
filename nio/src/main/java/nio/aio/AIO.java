package nio.aio;

import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步io   selector
 */
public class AIO {
    public static void client1() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("127.0.0.1.", 1234));
        socketChannel.configureBlocking(false);

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("hello this is client1. write once".getBytes());
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        socketChannel.close();
    }

    public static void client2() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", 1234));

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("hello this is client2. write in loop".getBytes());
        byteBuffer.flip();
        int i = 0;
        while(!socketChannel.finishConnect()){
            System.out.println("not connect yet" + i++);
        }
        while (byteBuffer.hasRemaining()) {
            socketChannel.write(byteBuffer);
        }

    }

    public static void main(String[] args) throws IOException {

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.DAYS, new LinkedBlockingDeque<>(1024), new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                System.out.println("thread pool create new thread . count " + threadNumber.get());
                return new Thread(r, "aio thread pool-" + threadNumber.getAndIncrement());
            }
        }, new ThreadPoolExecutor.AbortPolicy());
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.bind(new InetSocketAddress(1234));

                    Selector selector = Selector.open();
                    /**
                     * server socket只接受 accept
                     */
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                    while(selector.select() > 0) {
                        System.out.println("listen event");
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = iterator.next();
                            if (key.isAcceptable()) {
                                System.out.println("key is acceptable");
                            } else if (key.isConnectable()) {
                                System.out.println("key is connectable");
                            } else if (key.isReadable()) {
                                System.out.println("key is readable");
                            } else if (key.isWritable()) {
                                System.out.println("key is writeable");
                            }
                            iterator.remove();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });




    }
}
