package com.company.ops.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EngineeringOpsErpApplication {

  public static void main(String[] args) {
    SpringApplication.run(EngineeringOpsErpApplication.class, args);
  }
}
