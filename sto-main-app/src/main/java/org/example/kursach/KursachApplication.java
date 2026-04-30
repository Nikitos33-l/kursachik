package org.example.kursach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class KursachApplication {

    public static void main(String[] args) {
        SpringApplication.run(KursachApplication.class, args);
    }

}
