package org.yeyr2.as12306.userService.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 布隆过滤器配置
@Configuration
@EnableConfigurationProperties(UserRegisterBloomFilterProperties.class)
public class RBloomFilterConfiguration {
    /**
     * 防止用户注册缓存穿透的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> userRegisterCachePenetrationBloomFilter(RedissonClient redissonClient,UserRegisterBloomFilterProperties properties){
        RBloomFilter<String> filter = redissonClient.getBloomFilter(properties.getName());
        filter.tryInit(properties.getExpectedInsertions(),properties.getFalseProbability());
        return filter;
    }
}
