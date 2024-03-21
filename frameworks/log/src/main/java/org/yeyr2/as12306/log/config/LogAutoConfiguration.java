package org.yeyr2.as12306.log.config;

import org.springframework.context.annotation.Bean;
import org.yeyr2.as12306.log.core.ILogPrintAspect;

// 日志自动装配
public class LogAutoConfiguration {
    @Bean
    public ILogPrintAspect ILogPrintAspect(){
        return new ILogPrintAspect();
    }
}
