package io.repox.api3d.testconfig;

import io.repox.api3d.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class TestContainersDatabaseConfigurationForGenericIntegrationTest {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16.1-alpine")
                .withUrlParam("TC_TMPFS", "/testtmpfs:rw")
                .withDatabaseName("sbtemplate_generic_it")
                .withUsername("postgres")
                .withPassword("admin");
    }

    public static void main(String[] args) {
        SpringApplication.from(Main::main)
                .with(TestContainersDatabaseConfigurationForGenericIntegrationTest.class)
                .run(args);
    }
}
