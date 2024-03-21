package org.yeyr2.as12306.cache.core;

// 缓存过滤
@FunctionalInterface
public interface CacheGetFilter<T> {

    // 缓存过滤
    boolean filter(T param);
}
