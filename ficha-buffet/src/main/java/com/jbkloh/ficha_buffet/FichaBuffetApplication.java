package com.jbkloh.ficha_buffet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableWebSecurity 
public class FichaBuffetApplication {

	public static void main(String[] args) {
		SpringApplication.run(FichaBuffetApplication.class, args);
	}

}
