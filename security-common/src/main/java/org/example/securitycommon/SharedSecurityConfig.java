package org.example.securitycommon;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SharedSecurityConfig {

    @Bean
    public AuthHeaderFilter authHeaderFilter(){
        return new AuthHeaderFilter();
    }

}
