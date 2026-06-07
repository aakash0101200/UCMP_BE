package com.ucmp.ucmp_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class UcmpBackendApplication {

	@PostConstruct
	public void initTimezone() {
		// Force IST so every LocalDateTime.now() across the platform uses India time,
		// regardless of the cloud server's OS-level timezone (Render defaults to UTC).
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
	}

	public static void main(String[] args) {
		// Also set before Spring context initializes (covers static initializers)
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

		ApplicationContext context = SpringApplication.run(UcmpBackendApplication.class, args);
	}

}
