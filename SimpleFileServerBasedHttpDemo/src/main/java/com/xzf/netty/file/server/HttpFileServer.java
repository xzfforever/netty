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

    private final int port = 80;
    private final String localDir = "D:/log";

    public void run() throws Exception{
        EventLoopGroup acceptorGroup = new NioEventLoopGroup();
        EventLoopGroup clientGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(acceptorGroup,clientGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //HttpRequestDecoder将字节流缓存对象解码成POJO对象（HttpRequest、HttpContent等多个对象）
                            //HttpRequestEncoder将自定义业务Handlder中输出的FullHttpResponse对象编码成字节流
                            socketChannel.pipeline().addLast(new HttpServerCodec());
                            //HttpObjectAggregator将HttpRequest和HttpContent等多个对象（HttpRequestDecoder生成的）聚合成一个FullHttpRequest/FullHttpResponse对象
                            socketChannel.pipeline().addLast(new HttpObjectAggregator(64*1024));
                            //ChunkedWriteHandler:支持异步发送大的码流，但不占用过多的内存，防止发生Java内存溢出错误
                            socketChannel.pipeline().addLast(new ChunkedWriteHandler());
                            socketChannel.pipeline().addLast("http-handler",new HttpRequestHandler(localDir));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            System.out.println("HTTP文件目录服务器启动成功！访问端口号:"+port);
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
