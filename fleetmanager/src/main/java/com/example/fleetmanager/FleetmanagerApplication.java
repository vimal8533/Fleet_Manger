package com.example.fleetmanager;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.fleetmanager.model.Robot;
import com.example.fleetmanager.repository.RobotRepository;

@EnableScheduling
@SpringBootApplication
public class FleetmanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FleetmanagerApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(RobotRepository repository) {
		return args -> {
			if (repository.count() == 0) {
				repository.save(new Robot("R1", "A", 85, "IDLE"));
				repository.save(new Robot("R2", "B", 60, "MOVING"));
				repository.save(new Robot("R3", "C", 18, "CHARGING"));
				System.out.println("✅ Seeded initial robots into PostgreSQL database!");
			}
		};
	}
}
