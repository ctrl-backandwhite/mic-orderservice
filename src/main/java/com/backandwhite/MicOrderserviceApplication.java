package com.backandwhite;

import com.backandwhite.common.configuration.annotation.EnableCoreApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;

@EnableCoreApplication
@OpenAPIDefinition(servers = {@Server(url = "https://order-service.up.railway.app", description = "Production Server."),
        @Server(url = "https://localhost:6005", description = "Local Server.")})
public class MicOrderserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicOrderserviceApplication.class, args);
    }
}
