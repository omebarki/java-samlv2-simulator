package com.samlv2simulator.config;

import jakarta.annotation.PostConstruct;
import org.apache.xml.security.Init;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSamlConfig {

    @PostConstruct
    public void init() {
        Init.init();
    }
}
