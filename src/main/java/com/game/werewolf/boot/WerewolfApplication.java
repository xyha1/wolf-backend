package com.game.werewolf.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.game.werewolf")
public class WerewolfApplication {

    public static void main(String[] args) {
        SpringApplication.run(WerewolfApplication.class, args);
    }
}
