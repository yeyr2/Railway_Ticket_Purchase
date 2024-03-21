package org.yeyr2.as12306.cache;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.yeyr2.as12306.base.Singleton;
import org.yeyr2.as12306.cache.config.RedisDistributedProperties;
import org.yeyr2.as12306.cache.core.CacheGetFilter;
import org.yeyr2.as12306.cache.core.CacheGetIfAbsent;
import org.yeyr2.as12306.cache.core.CacheLoader;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;
import org.yeyr2.as12306.cache.toolkit.FastJson2Util;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// 分布式缓存之操作 Redis 模版代理
// 底层通过 {@link RedissonClient}、{@link StringRedisTemplate} 完成外观接口行为
@RequiredArgsConstructor
public class StringRedisTemplateProxy implements DistributedCache{

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisDistributedProperties redisDistributedProperties;
    private final RedissonClient redissonClient;
    private static final String LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH = "lua/putIfAllAbsent.lua";
    private static final String SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX = "safe_get_distributed_lock:";

    public Boolean hasKey(String key){
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (String.class.isAssignableFrom(clazz)) {
            return (T) value;
        }
        return JSON.parseObject(value, FastJson2Util.buildType(clazz));
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, redisDistributedProperties.getValueTimeout());
    }

    @Override
    public Boolean putIfAllAbsent(@NotNull Collection<String> keys) {
        DefaultRedisScript<Boolean> actual = Singleton.get(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH
                , () -> {
            DefaultRedisScript redisScript = new DefaultRedisScript();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH)));
            redisScript.setResultType(Boolean.class);
            return redisScript;
        });
        Boolean result = stringRedisTemplate.execute(actual, Lists.newArrayList(keys)
                , redisDistributedProperties.getValueTimeout().toString());
        return result != null && result;
    }

    @Override
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    @Override
    public Long delete(Collection<String> keys) {
        return stringRedisTemplate.delete(keys);
    }

    @Override
    public Object getInstance() {
        return stringRedisTemplate;
    }

    @Override
    public <T> T get(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return get(key,clazz,cacheLoader,timeout,redisDistributedProperties.getValueTimeUnit());
    }

    @Override
    public <T> T get(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit) {
        T result = get(key,clazz);
        if(!CacheUtil.isNullOrBlank(result)){
            return result;
        }
        return loadAndSet(key,cacheLoader,timeout,timeUnit,false,null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return safeGet(key,clazz,cacheLoader,timeout,redisDistributedProperties.getValueTimeUnit());
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit) {
        return safeGet(key, clazz, cacheLoader, timeout,timeUnit,null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         RBloomFilter<String> bloomFilter) {
        return safeGet(key, clazz, cacheLoader, timeout,bloomFilter,null,null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         TimeUnit timeUnit, RBloomFilter<String> bloomFilter) {
        return safeGet(key, clazz, cacheLoader, timeout,timeUnit,bloomFilter,null,null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter) {
        return safeGet(key, clazz, cacheLoader, timeout,redisDistributedProperties.getValueTimeUnit(),bloomFilter,cacheGetFilter,null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter) {
        return safeGet(key, clazz, cacheLoader, timeout,timeUnit,bloomFilter,cacheGetFilter,null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter,
                         CacheGetFilter<String> cacheGetFilter, CacheGetIfAbsent<String> cacheGetIfAbsent) {
        return safeGet(key, clazz, cacheLoader, timeout, redisDistributedProperties.getValueTimeUnit(), bloomFilter,cacheGetFilter,cacheGetIfAbsent);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter, CacheGetIfAbsent<String> cacheGetIfAbsent) {
        T result = get(key,clazz);
        // 缓存结果不等于空或空字符串直接返回；通过函数判断是否返回空，为了适配布隆过滤器无法删除的场景；两者都不成立，判断布隆过滤器是否存在，不存在返回空
        if(!CacheUtil.isNullOrBlank(result)
                || Optional.ofNullable(cacheGetFilter).map(each -> each.filter(key)).orElse(false)
                || Optional.ofNullable(bloomFilter).map(each -> !each.contains(key)).orElse(false)){
            return result;
        }
        RLock lock = redissonClient.getLock(SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX + key);
        lock.lock();
        try {
            // 双重判定锁，减轻获得分布式锁后线程访问数据库压力
            if(CacheUtil.isNullOrBlank(result = get(key,clazz))){
                // 如果访问 cacheLoader 加载数据为空，执行后置函数操作
                if(CacheUtil.isNullOrBlank(result = loadAndSet(key,cacheLoader,timeout,timeUnit,true,bloomFilter))){
                    Optional.ofNullable(cacheGetIfAbsent).ifPresent(each -> each.execute(key));
                }
            }
        }finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public void put(String key, Object value, long timeout) {
        put(key,value,timeout,redisDistributedProperties.getValueTimeUnit());
    }

    @Override
    public void put(String key, Object value, long timeout, TimeUnit timeUnit) {
        String actual = value instanceof String ? (String) value : JSON.toJSONString(value);
        stringRedisTemplate.opsForValue().set(key,actual,timeout,timeUnit);
    }

    @Override
    public void safePut(String key, Object value, long timeout, RBloomFilter<String> bloomFilter) {
        safePut(key,value,timeout,redisDistributedProperties.getValueTimeUnit(),bloomFilter);
    }

    @Override
    public void safePut(String key, Object value, long timeout, TimeUnit timeUnit, RBloomFilter<String> rBloomFilter) {
        put(key,value,timeout,timeUnit);
        if(rBloomFilter != null){
            rBloomFilter.add(key);
        }
    }

    @Override
    public Long countExistingKeys(@NotNull String... keys) {
        return stringRedisTemplate.countExistingKeys(Lists.newArrayList(keys));
    }

    // 按照CacheLoader的方法加载返回新值并保存
    private <T> T loadAndSet(String key,CacheLoader<T> cacheLoader,long timeout,TimeUnit timeUnit,
                             boolean safeFlag,RBloomFilter<String> bloomFilter ){
        T result = cacheLoader.load();
        if(CacheUtil.isNullOrBlank(result)){
            return result;
        }
        if(safeFlag){
            safePut(key,result,timeout,timeUnit,bloomFilter);
        }else {
            put(key,result,timeout,timeUnit);
        }
        return result;
    }
}