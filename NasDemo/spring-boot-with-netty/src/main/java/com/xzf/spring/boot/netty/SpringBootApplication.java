package com.xzf.spring.boot.netty;

import com.google.common.base.Charsets;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;

@org.springframework.boot.autoconfigure.SpringBootApplication
public class SpringBootApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SpringBootApplication.class);
		app.setWebEnvironment(false);
		app.run(args);
	}

	@Bean
	public HttpDataFactory httpDataFactory(){
		return new DefaultHttpDataFactory(DefaultHttpDataFactory.MAXSIZE);
	}

}
