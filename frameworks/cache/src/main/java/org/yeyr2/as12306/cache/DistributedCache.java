package org.yeyr2.as12306.cache;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.redisson.api.RBloomFilter;
import org.yeyr2.as12306.cache.core.CacheGetFilter;
import org.yeyr2.as12306.cache.core.CacheGetIfAbsent;
import org.yeyr2.as12306.cache.core.CacheLoader;
import java.util.concurrent.TimeUnit;

// 分布式缓存
public interface DistributedCache extends Cache{

    /**
     * 获取缓存,如查询结果为空,调用{@link CacheLoader} 加载缓存
     */
    <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout);

    <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存击穿、缓存雪崩场景，适用于不被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key,Class<T> clazz,CacheLoader<T> cacheLoader,long timeout);

    <T> T safeGet(@NotBlank String key,Class<T> clazz,CacheLoader<T> cacheLoader,long timeout,TimeUnit timeUnit);

    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter);

    <T> T safeGet(@NotBlank String key, Class<T> clazz,CacheLoader<T> cacheLoader, long timeout,TimeUnit timeUnit,RBloomFilter<String> bloomFilter);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，并通过 {@link CacheGetFilter} 解决布隆过滤器无法删除问题，适用于被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter);

    <T> T safeGet(@NotBlank String key,Class<T> clazz, CacheLoader<T> cacheLoader, long timeout
            ,TimeUnit timeUnit, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter);

    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout
            , RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter, CacheGetIfAbsent<String> cacheGetIfAbsent);

    <T> T safeGet(@NotBlank String key,Class<T> clazz,CacheLoader<T> cacheLoader,long timeout,TimeUnit timeUnit
            , RBloomFilter<String> bloomFilter,CacheGetFilter<String> cacheGetFilter,CacheGetIfAbsent<String> cacheGetIfAbsent);

    /**
     * 放入缓存，自定义超时时间
     */
    void  put(@NotBlank String key,Object value,long timeout);

    void put(@NotBlank String key,Object value,long timeout,TimeUnit timeUnit);

    /**
     * 放入缓存，自定义超时时间
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     */
    void safePut(@NotBlank String key,Object value,long timeout,RBloomFilter<String> bloomFilter);


    void safePut(@NotBlank String key,Object value,long timeout,TimeUnit timeUnit,RBloomFilter<String> rBloomFilter);

    // 统计指定key的存在数量
    Long countExistingKeys(@NotNull String... keys);
}
