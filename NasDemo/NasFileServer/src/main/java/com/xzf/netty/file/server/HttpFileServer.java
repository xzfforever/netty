package com.xzf.netty.file.server;

import com.xzf.netty.file.server.handler.HttpRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpFileServer {

    public static final int PORT = 9999;
    public static final String SERVER_NAME = "localhost";
    private final String localDir = "D:/log";


    public void run() throws Exception{
        EventLoopGroup acceptorGroup = new NioEventLoopGroup();
        EventLoopGroup clientGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(acceptorGroup,clientGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new HttpServerCodec());
                            socketChannel.pipeline().addLast(new HttpObjectAggregator(1024*1024));
                            socketChannel.pipeline().addLast("http-handler",new HttpRequestHandler(localDir));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(PORT).sync();
            System.out.println("HTTP文件目录服务器启动成功！访问端口号:"+PORT);
            channelFuture.channel().closeFuture().sync();
        }finally {
            acceptorGroup.shutdownGracefully();
            clientGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        new HttpFileServer().run();
    }

}
