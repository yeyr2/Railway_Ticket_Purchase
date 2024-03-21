package org.yeyr2.as12306.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

@Data
@ConfigurationProperties(prefix = RedisDistributedProperties.PREFIX)
public class RedisDistributedProperties {
    public static final String PREFIX = "framework.cache.redis";
    // key 前缀
    private String prefix = "";
    // key 前缀字符串
    private String prefixCharset = "UTF-8";
    // 默认超时时间
    private Long valueTimeout = 30000L;
    private TimeUnit valueTimeUnit = TimeUnit.MILLISECONDS;
}
