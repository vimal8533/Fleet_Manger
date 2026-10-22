package com.example.fleetmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FleetmanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FleetmanagerApplication.class, args);
	}

}
