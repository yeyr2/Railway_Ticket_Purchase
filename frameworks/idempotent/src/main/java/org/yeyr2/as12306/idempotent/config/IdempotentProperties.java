package org.yeyr2.as12306.idempotent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 幂等属性配置
@Data
@ConfigurationProperties(prefix = IdempotentProperties.PREFIX)
public class IdempotentProperties {

    public static final String PREFIX = "congomall.idempotent.token";

    // token幂等key前缀
    private String prefix;

    /**
     *     token申请后过期时间,单位默认毫秒{@link java.util.concurrent.TimeUnit#MILLISECONDS}随着分布式缓存过期时间单位{@link  org.yeyr2.as12306.cache.config.RedisDistributedProperties#valueTimeUnit}而变化
     */
    private Long timeout;
}
