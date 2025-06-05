package com.dailybread.reportanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ReportanalysisApplication {
	public static void main(String[] args) {
		SpringApplication.run(ReportanalysisApplication.class, args);
	}

}