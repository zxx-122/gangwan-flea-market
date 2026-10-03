package org.example.gwtzsc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GwtzscApplication {

    public static void main(String[] args) {
        SpringApplication.run(GwtzscApplication.class, args);
    }

}
