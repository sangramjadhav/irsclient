package com.harb.sj.irs.client.config;

import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import us.gov.treasury.irs.srv.acabulkrequesttransmitterstatus.ACATransmitterStatusReqPortType;
import us.gov.treasury.irs.srv.gettransmitterbulkrequest.BulkRequestTransmitterPortType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring configuration to provide WebService Client for Bulk request and status service
 */
@Configuration
public class WebServiceConfiguration implements EnvironmentAware {

    private RelaxedPropertyResolver propertyResolver;

    @Override
    public void setEnvironment(final Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, "webservice.");
    }


    @Bean(name="acaBulkRequestTransmitterService")
    public BulkRequestTransmitterPortType request() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(propertyResolver.getProperty("bulk.url"));
        factory.setServiceClass(BulkRequestTransmitterPortType.class);
        List<Feature> features = new ArrayList<>();
        features.add(new LoggingFeature());
        factory.setFeatures(features);
        Map<String, Object> properties = new HashMap<>();
        properties.put("schema-validation-enabled",
                propertyResolver.getProperty("bulk.properties.schema-validation-enabled", Boolean.class, true));
        properties.put("mtom-enabled",
                propertyResolver.getProperty("bulk.properties.mtom-enabled", Boolean.class, true));
        factory.setProperties(properties);
        BulkRequestTransmitterPortType client = (BulkRequestTransmitterPortType) factory.create();
        return client;
    }

    @Bean(name="acaTransmitterStatusService")
    public ACATransmitterStatusReqPortType status() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(propertyResolver.getProperty("status.url"));
        factory.setServiceClass(ACATransmitterStatusReqPortType.class);
        List<Feature> features = new ArrayList<>();
        features.add(new LoggingFeature());
        factory.setFeatures(features);
        Map<String, Object> properties = new HashMap<>();
        properties.put("schema-validation-enabled",
                propertyResolver.getProperty("status.properties.schema-validation-enabled", Boolean.class, true));
        factory.setProperties(properties);
        ACATransmitterStatusReqPortType client = (ACATransmitterStatusReqPortType) factory.create();
        return client;
    }
}
