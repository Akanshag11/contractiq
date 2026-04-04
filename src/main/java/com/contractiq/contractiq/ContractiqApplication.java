package com.contractiq.contractiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.contractiq")
@EnableJpaRepositories(basePackages = "com.contractiq")
public class ContractiqApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContractiqApplication.class, args);
	}

}
