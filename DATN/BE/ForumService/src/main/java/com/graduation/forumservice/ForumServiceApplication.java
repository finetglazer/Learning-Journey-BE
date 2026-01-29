package com.graduation.forumservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ForumServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForumServiceApplication.class, args);
    }

}
