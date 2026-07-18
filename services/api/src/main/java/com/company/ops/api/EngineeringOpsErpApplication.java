package com.company.ops.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class EngineeringOpsErpApplication {

  public static void main(String[] args) {
    SpringApplication.run(EngineeringOpsErpApplication.class, args);
  }
}
