package com.xzf.spring.boot.netty.handler;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.xzf.spring.boot.netty.constants.ServerConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.internal.StringUtil;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

@Component
@Scope("prototype")
@ChannelHandler.Sharable
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Value("${server.port}")
    private int port;

    @Value("${server.name}")
    private String serverName;

    @Value("${http.cache.seconds}")
    private int HTTP_CACHE_SECONDS;

    @Autowired
    private HttpDataFactory httpDataFactory;

    private HttpPostRequestDecoder decoder;

    static {
        DiskFileUpload.baseDirectory  =  "D:/log";
    }

    public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest)throws Exception{
        String  uri = fullHttpRequest.uri();
        System.out.println("request uri:"+uri);
        if (fullHttpRequest.decoderResult().isFailure()) {
            writeResponse(ctx,HttpResponseStatus.BAD_REQUEST,"failed");
            return;
        }
        if(uri.equals(ServerConstants.FAVICON_ICO)){
            writeResponse(ctx,HttpResponseStatus.OK,"success");
            return;
        }

        //上传文件
        if("/test/uploadFile".equals(uri)){
            dealWithContentType(ctx,fullHttpRequest);
        //获取资源
        }else if(uri.startsWith("/get")){
           String absoluteFilePath =  sanitizeUri(uri);
           System.out.println("-----absolute file path--------:"+absoluteFilePath);
            processDownload(absoluteFilePath,fullHttpRequest,ctx);
        }
    }

    private void processDownload(String absoluteFilePath,FullHttpRequest request,ChannelHandlerContext ctx) throws Exception {
        File file = new File(absoluteFilePath);
        if(!file.exists() || !file.isFile()){
            throw new Exception();
        }
        if(canUseCache(file,ctx,request)){
            return;
        }
        getFileFromLocal(file,request,ctx);
    }


    private void getFileFromLocal(File file,FullHttpRequest request,ChannelHandlerContext ctx) throws Exception{
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        long fileLength = raf.length();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setContentLength(response, fileLength);
        setDateAndCacheHeaders(response, file);
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set("CONNECTION", HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.write(response);
        ChannelFuture sendFileFuture;
        if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
        } else {
            sendFileFuture = ctx.write(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)), ctx.newProgressivePromise());
        }

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.out.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.out.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.out.println(future.channel() + " Transfer complete.");
            }
        });

        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        // Decide whether to close the connection or not.
        if (!HttpUtil.isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }



    private boolean canUseCache(File file,ChannelHandlerContext ctx,FullHttpRequest request) throws Exception{
        String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
        //检查请求头是否带有：If-Modified-Since标签
        if(StringUtil.isNullOrEmpty(ifModifiedSince)){
            return false;
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat(ServerConstants.HTTP_DATE_FORMAT, Locale.US);
        Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
        long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
        long fileLastModifiedSeconds = file.lastModified() / 1000;
        if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
            sendNotModified(ctx);
            return true;
        }
        return false;
    }


    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(ServerConstants.HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(ServerConstants.HTTP_DATE_GMT_TIMEZONE));
        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));
    }

    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }

    private static String sanitizeUri(String uri) {
        if (!uri.startsWith("/")) {
            return null;
        }

        if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri.startsWith(".") || uri.endsWith(".")
                || ServerConstants.INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

        String resourceReletivePath = uri.substring("/get".length());

        return DiskFileUpload.baseDirectory + resourceReletivePath;
    }

    private void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(ServerConstants.HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(ServerConstants.HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }



    private void dealWithContentType(ChannelHandlerContext ctx,FullHttpRequest request) throws Exception {
        String contentType = getContentType(request);
        String result = null;
        if(ServerConstants.MULTIPART_FORM_DATA.equals(contentType)){
            System.out.println("deal with file upload");
            result = processFileUpload(request);

        }else if(ServerConstants.APPLICATION_JSON.equals(contentType)){
            System.out.println("deal with json data");
        }
        System.out.println("deal success");
        writeResponse(ctx,HttpResponseStatus.OK,result);
    }

    private void writeResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String msg) throws Exception{
        ByteBuf byteBuf = Unpooled.copiedBuffer((msg+ServerConstants.CRLF).getBytes("UTF-8"));
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,status,byteBuf);
        fullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/html;charset=utf-8");
        ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
    }

    private boolean isClose(FullHttpRequest request){
        if(request.headers().contains(org.apache.http.HttpHeaders.CONNECTION, ServerConstants.CONNECTION_CLOSE, true) ||
                (request.protocolVersion().equals(HttpVersion.HTTP_1_0) &&
                        !request.headers().contains(org.apache.http.HttpHeaders.CONNECTION, ServerConstants.CONNECTION_KEEP_ALIVE, true)))
            return true;
        return false;
    }

    private String processFileUpload(FullHttpRequest request) throws Exception{
        initPostRequestDecoder(request);
        List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
        InterfaceHttpData fileInfo = getFileInfo(datas);
        Map<String, String> attributeInfo = getAttributeInfo(datas);
        checkAttributeInfo(attributeInfo);
        String url = processFile(attributeInfo,fileInfo);
        System.out.println("-----resource url is---:"+url);
        return url;

    }

    private String processFile(Map<String,String> attrMap,InterfaceHttpData fileData) throws Exception{
        String destDirStr = getDestDir(attrMap);
        File destDirFile = checkAndCreateDir(destDirStr);
        FileUpload fileUpload = (FileUpload) fileData;
        String fileName = fileUpload.getFilename();
        if(fileUpload.isCompleted()) {
            //保存到磁盘
            File destFile = new File(destDirFile, fileName);
            boolean fileCreateSuccess = fileUpload.renameTo(destFile);
            if(!fileCreateSuccess){
                throw new Exception("filed upload failed");
            }
            return buildUrl(destDirStr,fileName);
        }
        throw new Exception("filed upload failed");
    }

    private String buildUrl(String dir,String fileName){
        StringBuilder sb = new StringBuilder("http://").append(serverName)
                .append(":").append(port).append("/get");
        // TODO 增加Base64编码
        String reletiveDir = dir.substring(DiskFileUpload.baseDirectory.length());
        sb.append(reletiveDir).append("/").append(fileName);
        return sb.toString();
    }

    private void checkAttributeInfo(Map<String, String> attributeInfo) throws Exception{
        if(MapUtils.isEmpty(attributeInfo)){
            throw new Exception("invalid parameters");
        }
        String userName = attributeInfo.get("userName");
        String fileBizType = attributeInfo.get("bizType");
        Optional.ofNullable(userName).orElseThrow(Exception::new);
        Optional.ofNullable(fileBizType).orElseThrow(Exception::new);
    }

    private String getDestDir(Map<String,String> attrMap){
        String userName = attrMap.get("userName");
        String fileBizType = attrMap.get("bizType");
        StringBuilder sb = new StringBuilder(DiskFileUpload.baseDirectory);
        sb.append("/").append(userName).append("/").append(fileBizType);
        return sb.toString();
    }

    private File checkAndCreateDir(String destDir)throws Exception{
        File dir = new File(destDir);
        if(dir.isFile()){
            throw new Exception();
        }
        if(dir.exists()){
            return dir;
        }
        if(dir.mkdirs()){
            return dir;
        }
        throw new Exception("dir create error");
    }

    private InterfaceHttpData getFileInfo(List<InterfaceHttpData> datas){
        return Optional.ofNullable(datas).orElse(Lists.newArrayList())
                .stream()
                .filter(x->x.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload)
                .findAny().get();
    }



    private Map<String,String> getAttributeInfo(List<InterfaceHttpData> datas) throws IOException{
        List<Attribute> attrbuteList = datas.stream().filter(data->data.getHttpDataType() == (InterfaceHttpData.HttpDataType.Attribute))
                                     .map(x->(Attribute)x).collect(Collectors.toList());
        final Map<String,String>  resultMap = new HashMap<>();
        attrbuteList.stream().forEach(x->{
            String name = x.getName();
            String value = null;
            try {
                value= x.getValue();
            } catch (IOException e) {
                value = null;
            }
            resultMap.put(name,value);
        });
        return resultMap;
    }


    private void writeHttpData(InterfaceHttpData data) throws Exception{
        //后续会加上块传输（HttpChunk），目前仅简单处理
        if(data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) data;
            String fileName = fileUpload.getFilename();
            if(fileUpload.isCompleted()) {
                //保存到磁盘
                File destFile = new File(DiskFileUpload.baseDirectory, fileName);
                fileUpload.renameTo(destFile);
            }
        }
    }

    private void initPostRequestDecoder(FullHttpRequest request){
        if (decoder != null) {
            decoder.cleanFiles();
            decoder = null;
        }
        decoder = new HttpPostRequestDecoder(httpDataFactory, request, Charsets.UTF_8);
    }

    private String getContentType(FullHttpRequest request){
        String typeStr = request.headers().get("Content-Type");
        String[] list = typeStr.split(";");
        return list[0];
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
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
