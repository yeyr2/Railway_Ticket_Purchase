package org.yeyr2.as12306.base;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// 单例对象容器
public class Singleton {
    private static final ConcurrentHashMap<String,Object> SINGLE_OBJECT_POOL = new ConcurrentHashMap<>();

    private Singleton() {
    }

    // 根据key获取单例对象，如果为空就返回null
    public static <T> T get(String key){
        Object result = SINGLE_OBJECT_POOL.get(key);
        return result == null ? null : (T)result;
    }

    // 根据key获取单例对象，如果为空就构建对象
    public static  <T> T get(String key, Supplier<T> supplier){
        Object result = SINGLE_OBJECT_POOL.get(key);
        if(result == null && (result = supplier.get()) != null){
            SINGLE_OBJECT_POOL.put(key,result);
        }
        return result == null ? null : (T)result;
    }

    public static void put(Object value){
        put(value.getClass().getName(),value);
    }

    public static void put(String key,Object value){
        SINGLE_OBJECT_POOL.put(key,value);
    }
}
