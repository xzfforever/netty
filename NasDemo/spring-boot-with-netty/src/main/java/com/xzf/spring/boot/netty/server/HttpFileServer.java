package com.xzf.spring.boot.netty.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class HttpFileServer {

    @Autowired
    private ServerInitializer serverInitializer;

    @Value("${server.port}")
    private int port;

    private EventLoopGroup acceptorGroup;
    private  EventLoopGroup clientGroup;
    private Channel channel;

    @PostConstruct
    public void serverStart() throws Exception{
         acceptorGroup = new NioEventLoopGroup();
         clientGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(acceptorGroup,clientGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(serverInitializer);
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            channelFuture.channel();
            System.out.println("HTTP文件目录服务器启动成功！访问端口号:"+port);
            channelFuture.channel().closeFuture().sync();
        }finally {
            acceptorGroup.shutdownGracefully();
            clientGroup.shutdownGracefully();
        }
    }

    @PreDestroy
    public void stop() {
        System.out.println("destroy server resources");
        acceptorGroup.shutdownGracefully();
        clientGroup.shutdownGracefully();
        channel.closeFuture().syncUninterruptibly();
        acceptorGroup = null;
        clientGroup = null;
        channel = null;
    }

}
