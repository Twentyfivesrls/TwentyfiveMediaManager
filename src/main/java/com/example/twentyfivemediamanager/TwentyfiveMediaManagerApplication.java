package com.example.twentyfivemediamanager;

import com.example.twentyfivemediamanager.config.FileSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(FileSecurityProperties.class)
@SpringBootApplication
public class TwentyfiveMediaManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TwentyfiveMediaManagerApplication.class, args);
    }

}
