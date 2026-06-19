package com.stepside.StepSide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class StepSideApplication {
	public static void main(String[] args) {
		SpringApplication.run(StepSideApplication.class, args);
	}
}
