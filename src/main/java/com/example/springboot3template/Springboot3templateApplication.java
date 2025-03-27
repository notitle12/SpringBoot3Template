package com.example.springboot3template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class})
@EnableJpaAuditing
public class Springboot3templateApplication {

    public static void main(String[] args) {
        SpringApplication.run(Springboot3templateApplication.class, args);
    }

}
