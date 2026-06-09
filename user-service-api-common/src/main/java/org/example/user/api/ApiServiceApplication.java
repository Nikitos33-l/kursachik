package org.example.user.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ApiServiceApplication {

    public static void main(String[] args) {
        PasswordEncoder bcrypt = new BCryptPasswordEncoder();
        System.out.println("Чистый BCrypt: " + bcrypt.encode("admin"));

        //SpringApplication.run(ApiServiceApplication.class, args);
    }

}
