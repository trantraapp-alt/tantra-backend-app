package com.hyperlocal.tantra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class TantraApplication {

	public static void main(String[] args) {
		SpringApplication.run(TantraApplication.class, args);
	}

}
