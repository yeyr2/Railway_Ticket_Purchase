package org.yeyr2.as12306.cache;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;

// 缓存接口
public interface Cache {
    //获取缓存
    <T> T get(@NotBlank String key, Class<T> clazz);

    // 放入缓存
    void put(@NotBlank String key,Object value);

    // 如果keys全部不存在,则新增,返回true,反之false
    Boolean putIfAllAbsent(@NotNull Collection<String> keys);

    // 删除缓存
    Boolean delete(@NotBlank String key);

    // 删除keys,返回删除数量
    Long delete(@NotNull Collection<String> keys);

    // 判断Key是否存在
    Boolean hasKey(@NotBlank String key);

    // 获取缓存组建实例
    Object getInstance();
}
