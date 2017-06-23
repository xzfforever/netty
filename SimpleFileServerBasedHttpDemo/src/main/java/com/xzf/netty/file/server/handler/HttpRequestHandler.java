package com.xzf.netty.file.server.handler;

import com.sun.deploy.util.StringUtils;
import com.xzf.netty.file.server.constants.ErrorInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.nio.file.Files.readAllBytes;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private String localDir;
    private static final String CRLF = System.getProperty("line.separator");
    private static SimpleDateFormat sdf=new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

    public HttpRequestHandler(String localDir){
        this.localDir = localDir;
    }

    public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest req)throws Exception{
        if(req.decoderResult().isFailure()){
            sendErrorToClient(ctx,HttpResponseStatus.BAD_REQUEST);
            return;
        }
        if(req.method().compareTo(HttpMethod.GET)!=0){
            sendErrorToClient(ctx,HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }
        System.out.println("Http Request Uri:"+req.uri());

        String filePath = getRequestFilePath(req);
        if(StringUtil.isNullOrEmpty(filePath)){
            sendErrorToClient(ctx,HttpResponseStatus.BAD_REQUEST);
            return;
        }

        processFile(ctx,req,filePath);
        ctx.close();
    }

    private void processFile(ChannelHandlerContext ctx, FullHttpRequest req,String filePath) throws Exception{
        File file = new File(filePath);
        if(!file.exists()){
            sendErrorToClient(ctx,HttpResponseStatus.NOT_FOUND);
            return;
        }
        if(file.isFile()){
            sendFileToClient(ctx,file,req);
            return;
        }
        if(file.isDirectory()){
            sendDirListToClient(ctx,file,req);
            return;
        }
    }

    private void sendDirListToClient(ChannelHandlerContext ctx, File dir, FullHttpRequest req) throws Exception{
        StringBuffer sb = new StringBuffer("");
        String currentDir = dir.getPath();
        String currentUri = req.uri();
        sb.append("<!DOCTYPE HTML>"+CRLF);
        sb.append("<html><head><title>");
        sb.append(currentDir);
        sb.append("目录：");
        sb.append("</title></head><body>"+CRLF);
        sb.append("<h3>");
        sb.append("当前目录:"+currentDir);
        sb.append("</h3>");
        sb.append("<table>");
        sb.append("<tr><td colspan='3'>上一级:<a href=\"../\">..</a>  </td></tr>");
        if(currentUri.equals("/")){
            currentUri = "";
        }else{
            if(currentUri.charAt(0)=='/'){
                currentUri = currentUri.substring(0);
            }
            currentUri += "/";
        }
        String fnameShow;
        for (File f:dir.listFiles()) {
            if(f.isHidden()||!f.canRead()){
                continue;
            }
            String fname=f.getName();
            Calendar cal=Calendar.getInstance();
            cal.setTimeInMillis(f.lastModified());
            String lastModified=sdf.format(cal.getTime());
            sb.append("<tr>");
            if(f.isFile()){
                fnameShow="<font color='green'>"+fname+"</font>";
            }else {
                fnameShow="<font color='red'>"+fname+"</font>";
            }
            sb.append("<td style='width:200px'> "+lastModified+"</td><td style='width:100px'>"+Files.size(f.toPath())+"</td><td><a href=\""+currentUri+fname+"\">"+fnameShow+"</a></td>");
            sb.append("</tr>");
        }
        sb.append("</table>").append("</body></html>");
        System.out.println("Response Data："+sb.toString());
        ByteBuf buffer=Unpooled.copiedBuffer(sb.toString(), CharsetUtil.UTF_8);
        FullHttpResponse resp=new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK,buffer);
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendFileToClient(ChannelHandlerContext ctx, File file, FullHttpRequest req) throws Exception{
        ByteBuf byteBuf = Unpooled.copiedBuffer(readAllBytes(file.toPath()));
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK,byteBuf);
        MimetypesFileTypeMap mimeTypeMap=new MimetypesFileTypeMap();
        req.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypeMap.getContentType(file));
        ctx.writeAndFlush(req).addListener(ChannelFutureListener.CLOSE);
    }





    private String getRequestFilePath(FullHttpRequest request){
        try {
            String uri = request.uri();
            uri = URLDecoder.decode(uri, "UTF-8");
            return localDir+uri;
        }catch (Exception e){
            return null;
        }
    }


    private void sendErrorToClient(ChannelHandlerContext context, HttpResponseStatus status) throws Exception{
        ByteBuf byteBuf = Unpooled.copiedBuffer((ErrorInfo.SYSTEM_ERROR+":"+status.toString()+CRLF).getBytes("UTF-8"));
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,status,byteBuf);
        fullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/html;charset=utf-8");
        context.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        messageReceived(channelHandlerContext,fullHttpRequest);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
