package com.example.dingtalk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DingTalkApplication {

    public static void main(String[] args) {
        SpringApplication.run(DingTalkApplication.class, args);
    }

}
