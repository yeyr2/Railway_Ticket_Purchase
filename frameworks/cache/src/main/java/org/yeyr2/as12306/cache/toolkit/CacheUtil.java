package org.yeyr2.as12306.cache.toolkit;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.util.Optional;
import java.util.stream.Stream;

// 缓存工具类
public final class CacheUtil {
    private static final String SPLICING_OPERATOR = "_";

    /**
     * 构建缓存标识 <p>
     * 像 StrUtil.join("_",...);
     */
    public static String buildKey(String... keys){
        Stream.of(keys)
                .forEach(each -> Optional
                        .ofNullable(Strings.emptyToNull(each))
                        .orElseThrow(()->new RuntimeException("构建缓存 key 不允许为空")
                        )
                );
        return Joiner.on(SPLICING_OPERATOR).join(keys);
    }

    // 判断结果是否为空或空的字符串
    public static boolean isNullOrBlank(Object cacheVal){
        return cacheVal == null || (cacheVal instanceof String && Strings.isNullOrEmpty((String) cacheVal));
    }
}
