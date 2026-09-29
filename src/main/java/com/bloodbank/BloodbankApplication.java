package com.bloodbank;

import com.bloodbank.config.BloodBankProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(BloodBankProperties.class)
public class BloodbankApplication {

	public static void main(String[] args) {
		SpringApplication.run(BloodbankApplication.class, args);
	}

}
