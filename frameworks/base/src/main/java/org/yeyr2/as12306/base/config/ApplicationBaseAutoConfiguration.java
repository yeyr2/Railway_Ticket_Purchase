package org.yeyr2.as12306.base.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.yeyr2.as12306.base.ApplicationContextHolder;
import org.yeyr2.as12306.base.init.ApplicationContentPostProcessor;
import org.yeyr2.as12306.base.safe.FastJsonSafeMode;

public class ApplicationBaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextHolder congoApplicationContextHolder(){
        return new ApplicationContextHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContentPostProcessor congoApplicationContentPostProcessor(ApplicationContext applicationContext){
        return new ApplicationContentPostProcessor(applicationContext);
    }

    // 只有JsonSafe模式为true时，才会创建
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "framework.fastjson.safa-mode",havingValue = "true")
    public FastJsonSafeMode congoFastJsonSafeMode(){
        return new FastJsonSafeMode();
    }
}
