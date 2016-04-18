package com.harb.sj.irs.client.config;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring configuration to load Application Config
 */
@Configuration
public class ApplicationConfiguration implements EnvironmentAware {

    private RelaxedPropertyResolver propertyResolver;

    @Override
    public void setEnvironment(final Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, "config.");
    }

    @Bean
    public ApplicationConfig applicationConfig(){
        final ApplicationConfig config = new ApplicationConfig();
        config.setTccId(propertyResolver.getProperty("tcc-id", ""));
        return config;
    }
}
