package com.xzf.time.server;


import com.xzf.time.server.handler.TimeServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class TimeServer {

    public static void main(String[] args) throws Exception{
        int port = 8080;
        new TimeServer().bind(port);
    }

    private void bind(int port) throws Exception{
        //NioEventLoopGroup是个线程组（Reactor线程组），用于网络事件的处理
        //bossGroup线程组：处理服务端接受客户端的链接
        //workerGroup线程组：进行SocketChannel的网络读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //ServerBootstrap对象是Netty用于启动NIO服务端的辅助启动类
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)//NioServerSocketChannel相当于NIO中的ServerSocketChannel类
                    .option(ChannelOption.SO_BACKLOG, 1024)//设置Tcp参数
                    .childHandler(new ChildChannelHandler());//绑定IO事件的处理类：ChildChannelHandler
            ChannelFuture channelFuture = bootstrap.bind(port).sync();//调用同步阻塞方法sync，等待绑定操作完成
            //ChannelFuture：用于异步操作的通知回调
            channelFuture.channel().closeFuture().sync();//等待服务端链路关闭之后，main函数再退出
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    /**
     * LineBasedFrameDecoder的工作原理是依次遍历ByteBuf中的可读字节，判断是否有"\n"或"\r\n",如果有则以此为结束位置
     * StringDecoder:将接收到的对象转换成字符串
     * DelimiterBasedFrameDecoder:以分隔符做结束标志的消息的解码
     * FixedLengthFrameDecoder: 对定长消息解码
     */
    private class ChildChannelHandler extends ChannelInitializer<SocketChannel>{

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
            socketChannel.pipeline().addLast(new StringDecoder());
            socketChannel.pipeline().addLast(new TimeServerHandler());
        }
    }


}
