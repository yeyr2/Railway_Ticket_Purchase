package org.yeyr2.as12306.common.toolkit;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;
import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.dozermapper.core.loader.api.TypeMappingOptions.mapEmptyString;
import static com.github.dozermapper.core.loader.api.TypeMappingOptions.mapNull;

// 对象属性复制工具类
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanUtil {
    protected static Mapper BEAN_MAPPER_BUILDER;
    static {
        BEAN_MAPPER_BUILDER = DozerBeanMapperBuilder.buildDefault();
    }

    // 属性复制
    public static <T, S> T convert(S source,T target){
        Optional.ofNullable(source)
                .ifPresent(each -> BEAN_MAPPER_BUILDER.map(each,target));
        return target;
    }

    // 复制单个对象
    public static <T, S> T convert(S source,Class<T> clazz){
        return Optional.ofNullable(source)
                .map(each -> BEAN_MAPPER_BUILDER.map(each,clazz))
                .orElse(null);
    }

    // 复制多个对象
    public static <T,S> List<T> convert(List<S> source,Class<T> clazz){
        return Optional.ofNullable(source)
                .map(each -> {
                    List<T> targetList = new ArrayList<>(each.size());
                    each.stream().forEach(item -> targetList.add(BEAN_MAPPER_BUILDER.map(item,clazz)));
                    return targetList;
                })
                .orElse(null);
    }

    // 复制多个对象
    public static <T, S> T[] convert(S[] sources,Class<T> clazz){
        return Optional.ofNullable(sources)
                .map(each -> {
                    @SuppressWarnings("unchecked")
                    T[] targetArray = (T[]) Array.newInstance(clazz,sources.length);
                    for(int i = 0 ; i < targetArray.length ; i++){
                        targetArray[i] = BEAN_MAPPER_BUILDER.map(sources[i],clazz);
                    }
                    return targetArray;
                }).orElse(null);
    }

    // 拷贝非空且非空串属性
    public static void convertIgnoreNullAndBlank(Object source,Object target){
        DozerBeanMapperBuilder dozerBeanMapperBuilder = DozerBeanMapperBuilder.create();
        Mapper mapper = dozerBeanMapperBuilder.withMappingBuilders(new BeanMappingBuilder() {
            @Override
            protected void configure() {
                mapping(source.getClass(),target.getClass(),mapNull(false),mapEmptyString(false));
            }
        }).build();
        mapper.map(source,target);
    }

    // 拷贝非空属性
    public static void convertIgnoreNull(Object source,Object target){
        DozerBeanMapperBuilder dozerBeanMapperBuilder = DozerBeanMapperBuilder.create();
        Mapper mapper = dozerBeanMapperBuilder.withMappingBuilders(new BeanMappingBuilder() {
            @Override
            protected void configure() {
                mapping(source.getClass(),target.getClass(),mapNull(false));
            }
        }).build();
        mapper.map(source,target);
    }
}
