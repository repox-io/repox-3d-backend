package io.repox.api3d;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("io.repox.api3d.appconfig.options")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
