package org.yeyr2.as12306.distributedid.core.snowflake;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.yeyr2.as12306.base.ApplicationContextHolder;

import java.util.ArrayList;
import java.util.List;

// 使用Redis获取雪花WorkId
@Slf4j
public class LocalRedisWorkIdChoose extends AbstractWorkIdChooseTemplate implements InitializingBean {

    private final RedisTemplate stringRedisTemplate;

    public LocalRedisWorkIdChoose() {
        this.stringRedisTemplate = ApplicationContextHolder.getBean(StringRedisTemplate.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        chooseAndInit();
    }

    @Override
    protected WorkIdWrapper chooseWorkId() {
        DefaultRedisScript redisScript = new DefaultRedisScript();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/chooseWorkIdLua.lua")));
        List<Long> luaResultList = null;
        try{
            redisScript.setResultType(List.class);
            luaResultList = (ArrayList)this.stringRedisTemplate.execute(redisScript,null);
        }catch (Exception ex){
            log.error("Redis Lua 脚本获取 WorkId 失败",ex);
        }
        return CollUtil.isNotEmpty(luaResultList) ? new WorkIdWrapper(luaResultList.get(0), luaResultList.get(1)) : new RandomWorkIdChoose().chooseWorkId();
    }
}
