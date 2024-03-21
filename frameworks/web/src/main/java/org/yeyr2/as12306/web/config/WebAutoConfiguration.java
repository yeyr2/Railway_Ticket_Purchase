package org.yeyr2.as12306.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.yeyr2.as12306.web.GlobalExceptionHandler;
import org.yeyr2.as12306.web.initialize.InitializeDispatcherServletController;
import org.yeyr2.as12306.web.initialize.InitializeDispatcherServletHandler;

// web组件自动装配
public class WebAutoConfiguration {
    public final static String INITIALIZE_PATH = "/initialize/dispatcher-servlet";

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(){
        return new GlobalExceptionHandler();
    }

    @Bean
    public InitializeDispatcherServletController initializeDispatcherServletController(){
        return new InitializeDispatcherServletController();
    }

    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory(){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(5000);
        factory.setChunkSize(5000);
        return factory;
    }

    @Bean
    @ConditionalOnBean(ClientHttpRequestFactory.class)
    public RestTemplate simpleRestTemplate(ClientHttpRequestFactory factory){
        return new RestTemplate(factory);
    }

    @Bean
    @ConditionalOnBean({RestTemplate.class, ConfigurableEnvironment.class})
    public InitializeDispatcherServletHandler initializeDispatcherServletHandler(RestTemplate simpleRestTemplate, ConfigurableEnvironment configurableEnvironment){
        return new InitializeDispatcherServletHandler(simpleRestTemplate,configurableEnvironment);
    }
}
