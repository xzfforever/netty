package com.xzf.netty.file.server.handler;

import org.apache.commons.codec.binary.Base64;


public class Test {

    public static void main(String args[]) throws Exception{

        String baseDir = "d:/log";
        String fullDir = "d:/log/xiezhenfeng538/img/xxxx.img";
        String resultStr = fullDir.substring(baseDir.length());
        byte[] encodeBytes = Base64.encodeBase64(resultStr.getBytes("UTF-8"));
        System.out.println("encode string:"+new String(encodeBytes));
        byte[] decodeBytes = Base64.decodeBase64(encodeBytes);
        System.out.println("decode string:"+new String(decodeBytes));
        System.out.println("-------------------");

    }

}
