package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class DemoApplication {

	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	public static void main(String[] args) {
		logger.info("========================================");
		logger.info("Starting Spring Boot Application...");
		logger.info("========================================");
		SpringApplication.run(DemoApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		logger.info("========================================");
		logger.info("Application is READY and RUNNING!");
		logger.info("Server is listening on http://0.0.0.0:8080");
		logger.info("Available endpoints:");
		logger.info("  - GET  http://localhost:8080/api/hello");
		logger.info("  - GET  http://localhost:8080/api/messages");
		logger.info("  - POST http://localhost:8080/api/messages");
		logger.info("========================================");
	}

}
