package org.yeyr2.as12306.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = BloomFilterPenetrateProperties.PREDIX)
public class BloomFilterPenetrateProperties {
    public static final String PREDIX = "framework.cache.redis.bloom-filter.default";
    // 布隆过滤器默认实例名称
    private String name = "cache_default_bloom_filter";
    //  每个元素的预期插入量
    private Long expectedInsertions = 64000L;
    //预期错误概率
    private Double falseProbability = 0.030;
}
