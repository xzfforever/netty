package com.xzf.time.server.handler;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;


public class TimeServerHandler extends ChannelInboundHandlerAdapter {

    private static final String QUERY_TIME_COMMAND = "QUERY TIME COMMAND";

    private static final String ERROR_COMMAND = "ERROR_COMMAND";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf)msg;
        byte[] msgBytes = new byte[buf.readableBytes()];
        buf.readBytes(msgBytes);
        String msgBody = new String(msgBytes,"UTF-8");
        System.out.println("The time server received data:"+msgBody);
        String currentTime = QUERY_TIME_COMMAND.equalsIgnoreCase(msgBody)? new Date().toString() : "ERROR_COMMAND";
        ByteBuf responseData = Unpooled.copiedBuffer(currentTime.getBytes());
        //从性能角度考虑，为了防止频繁地唤醒Selector进行消息发送，Netty的write方法并不直接将消息写入
        //SocketChannel中，调用write方法只是把待发送的消息发送缓冲数组中，再通过调用flush方法,将发送缓冲区中的全部消息写到SocketChannel中。
        ctx.write(responseData);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
