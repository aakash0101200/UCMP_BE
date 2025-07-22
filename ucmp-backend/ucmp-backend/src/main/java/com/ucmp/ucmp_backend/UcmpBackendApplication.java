package com.ucmp.ucmp_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class UcmpBackendApplication {

	
	public static void main(String[] args) {
		
		ApplicationContext context = SpringApplication.run(UcmpBackendApplication.class, args);
	}

}
