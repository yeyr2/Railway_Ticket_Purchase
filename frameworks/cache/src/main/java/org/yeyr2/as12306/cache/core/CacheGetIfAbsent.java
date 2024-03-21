package org.yeyr2.as12306.cache.core;

@FunctionalInterface
public interface CacheGetIfAbsent<T> {

    // 缓存查询为空，则执行
    void execute(T param);
}
