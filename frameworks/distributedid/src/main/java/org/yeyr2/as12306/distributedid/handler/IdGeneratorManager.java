package org.yeyr2.as12306.distributedid.handler;

import lombok.NonNull;
import org.yeyr2.as12306.distributedid.core.IdGenerator;
import org.yeyr2.as12306.distributedid.core.serviceid.DefaultServiceIdGenerator;
import org.yeyr2.as12306.distributedid.core.serviceid.ServiceIdGenerator;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

//id 生成器管理
public class IdGeneratorManager {
    // ID生成器管理容器
    private static Map<String, IdGenerator> MANAGER = new ConcurrentHashMap<>();

    // 注册默认ID生成器
    static {
        MANAGER.put("default",new DefaultServiceIdGenerator());
    }

    // 注册ID生成器
    public static void registerIdGenerator(@NonNull String resource,@NonNull IdGenerator idGenerator){
        IdGenerator actual = MANAGER.get(resource);
        if(actual != null){
            return;
        }
        MANAGER.put(resource,idGenerator);
    }

    //根据@param resource获取ID生成器
    public static ServiceIdGenerator getIdGenerator(@NonNull String resource){
        return Optional.ofNullable(MANAGER.get(resource)).map(each -> (ServiceIdGenerator)each).orElse(null);
    }

    /**
     * 获取默认ID生成器{@Link DefaultServiceIdGenerator}
     */
    public static ServiceIdGenerator getDefaultServiceIdGenerator(){
        return Optional.ofNullable(MANAGER.get("default")).map(each -> (ServiceIdGenerator)each).orElse(null);
    }
}
