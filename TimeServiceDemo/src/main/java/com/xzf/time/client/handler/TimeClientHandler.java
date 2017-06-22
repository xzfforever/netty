package com.xzf.time.client.handler;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TimeClientHandler extends ChannelInboundHandlerAdapter{

    private final ByteBuf commandMessage;

    private static final String QUERY_TIME_COMMAND = "QUERY TIME COMMAND";

    public TimeClientHandler(){
        byte[] req = QUERY_TIME_COMMAND.getBytes();
        commandMessage = Unpooled.buffer(req.length);
        commandMessage.writeBytes(req);
    }

    //当客户端与服务端的TCP链路建立成功之后，Netty会优先调用channelActive方法
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(commandMessage);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf)msg;
        byte[] msgBytes = new byte[buf.readableBytes()];
        buf.readBytes(msgBytes);
        String msgContent = new String(msgBytes,"UTF-8");
        System.out.println("Now is :"+ msgContent);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

}
