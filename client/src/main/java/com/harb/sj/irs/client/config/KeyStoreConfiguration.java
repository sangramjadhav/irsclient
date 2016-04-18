package com.harb.sj.irs.client.config;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring configuration to load Keystore
 */
@Configuration
public class KeyStoreConfiguration implements EnvironmentAware {

    private RelaxedPropertyResolver propertyResolver;

    @Override
    public void setEnvironment(final Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, "keystore.");
    }

    @Bean
    public KeyStoreConfig keyStoreConfig(){
        final String ksPath = propertyResolver.getProperty("path", "");
        final String ksType = propertyResolver.getProperty("type", "JKS");
        final String ksPassword = propertyResolver.getProperty("password", "");
        final String ksPKpassword = propertyResolver.getProperty("private-key.password", "");
        final String pkAlias = propertyResolver.getProperty("private-key.alias", "");
        final int index = propertyResolver.getProperty("certificate-index", Integer.class, 0);
        final KeyStoreConfig keyStoreConfig = new KeyStoreConfig();
        keyStoreConfig.setPath(ksPath);
        keyStoreConfig.setType(ksType);
        keyStoreConfig.setStorePassword(ksPassword);
        keyStoreConfig.setKeyPassword(ksPKpassword);
        keyStoreConfig.setKeyAlias(pkAlias);
        keyStoreConfig.setCertificateIndex(index);
        return keyStoreConfig;
    }
}
