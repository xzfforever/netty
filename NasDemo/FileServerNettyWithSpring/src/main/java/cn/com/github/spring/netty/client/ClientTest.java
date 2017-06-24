package cn.com.github.spring.netty.client;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 类的描述信息
 *
 * @author panzhuowen
 * @version 1.0.1
 */
public class ClientTest {

    private static final int SERVER_PORT = 9527;

    @Test
    public void testClient1() throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",SERVER_PORT));
//        socketChannel.configureBlocking(false);
        String message = "testClient1 say hello";
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put(message.getBytes());
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        socketChannel.close();

}

    @Test
    public void testClient2() throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",SERVER_PORT));
        String message = "testClient2 say world";
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put(message.getBytes());
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();
        socketChannel.read(byteBuffer);
        Charset charset = StandardCharsets.UTF_8;
        CharsetDecoder charsetDecoder = charset.newDecoder();
        byteBuffer.flip();
        charsetDecoder.decode(byteBuffer);
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < byteBuffer.limit(); i++) {
            stringBuffer.append((char)byteBuffer.get(i));
        }
        System.out.println(stringBuffer.toString());
        socketChannel.close();

    }

}
