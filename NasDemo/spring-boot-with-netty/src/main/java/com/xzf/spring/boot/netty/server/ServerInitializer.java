package com.xzf.spring.boot.netty.server;

import com.xzf.spring.boot.netty.handler.HttpRequestHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private HttpRequestHandler httpRequestHandler;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new HttpServerCodec());
        socketChannel.pipeline().addLast(new HttpObjectAggregator(10*1024*1024));
        socketChannel.pipeline().addLast("http-handler",httpRequestHandler);
    }
}
