package com.xzf.spring.boot.netty.constants;

import java.util.Base64;

/**
 * Created by Administrator on 2017/6/25.
 */
public class Test {

    public static void main(String args[]){
        String fileName = "hello.txt";

        System.out.println(fileName.substring(0,fileName.lastIndexOf(".")));

        System.out.println(fileName.substring(fileName.lastIndexOf("."),fileName.length()));

        String str = "/get/L3hpZXpoZW5mZW5nNTM4L2RldGFpbGxvZ2NvbmZpZw==.ini";

        String encodeStr =  str.substring("/get/".length(),str.lastIndexOf("."));
        System.out.println("encodestr : "+encodeStr);
        System.out.println(new String(Base64.getDecoder().decode(encodeStr)));

    }


}
