package com.example.FamilyHub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@SpringBootApplication
@EnableWebFlux
@EnableDiscoveryClient
@EnableFeignClients
public class FamilyHubApplication implements WebFluxConfigurer {

	public static void main(String[] args) {

		SpringApplication.run(FamilyHubApplication.class, args);
	}

	@Bean
	public WebClient webClient() {

		return WebClient.builder().build();
	}

}

