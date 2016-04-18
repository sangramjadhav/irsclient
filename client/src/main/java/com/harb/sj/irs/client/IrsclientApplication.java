package com.harb.sj.irs.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IrsclientApplication {

	public static void main(String[] args) {
		SpringApplication.run(IrsclientApplication.class, args).close();
		System.exit(0);
	}
}
