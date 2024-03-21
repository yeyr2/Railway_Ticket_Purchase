package org.yeyr2.as12306.designpattern.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.yeyr2.as12306.base.config.ApplicationBaseAutoConfiguration;
import org.yeyr2.as12306.designpattern.chain.AbstractChainContext;
import org.yeyr2.as12306.designpattern.strategy.AbstractStrategyChoose;

// 设计模式自动装配
@ImportAutoConfiguration(ApplicationBaseAutoConfiguration.class)
public class DesignPatternAutoConfiguration {
    /**
     * 策略模式选择器
     */
    @Bean
    public AbstractStrategyChoose abstractStrategyChoose() {
        return new AbstractStrategyChoose();
    }

    /**
     * 责任链上下文
     */
    @Bean
    public AbstractChainContext abstractChainContext() {
        return new AbstractChainContext();
    }
}
