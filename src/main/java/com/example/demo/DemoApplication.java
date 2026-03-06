package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Algorithm Visualization backend.
 * Boots the web server and initializes all controllers/services.
 */
@SpringBootApplication
public class DemoApplication {

	/**
     * Application main method.
     * Starts the Spring Boot runtime and embedded server.
     */
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
