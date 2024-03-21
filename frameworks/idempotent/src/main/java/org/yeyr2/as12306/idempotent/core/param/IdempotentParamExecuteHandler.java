package org.yeyr2.as12306.idempotent.core.param;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.yeyr2.as12306.bizs.user.core.UserContext;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.idempotent.core.AbstractIdempotentExecuteHandler;
import org.yeyr2.as12306.idempotent.core.IdempotentContext;
import org.yeyr2.as12306.idempotent.core.IdempotentParamWrapper;

// 基于方法参数验证请求幂等性
@RequiredArgsConstructor
public final class IdempotentParamExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentParamService{

    private final RedissonClient redissonClient;

    private final static String LOCK = "lock:param:restAPI";

    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        String lockKey = String.format("idempotent:path:%s;currentUserId:%s;sah512:%s",getServletPath(),getCurrentUserId(),calcArgsSha512Hex(joinPoint));
        return IdempotentParamWrapper
                .builder()
                .lockKey(lockKey)
                .joinPoint(joinPoint)
                .build();
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        String lockKey = wrapper.getLockKey();
        RLock lock = redissonClient.getLock(lockKey);
        if(!lock.tryLock()){
            throw new ClientException(wrapper.getIdempotent().message());
        }
        IdempotentContext.put(LOCK,lock);
    }

    @Override
    public void exceptionProcessing() {
        super.exceptionProcessing();
    }

    @Override
    public void postProcessing() {
        RLock lock = null;
        try{
            lock = (RLock) IdempotentContext.getKey(LOCK);
        }finally {
            if(lock != null){
                lock.unlock();
            }
        }
    }

    /**
     * 获取当前线程上下文ServletPath
     */
    private String getServletPath(){
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra != null) {
            return sra.getRequest().getServletPath();
        }
        return null;
    }

    /**
     * 获取当前操作用户ID
     */
    private String getCurrentUserId(){
        String userId = UserContext.getUserId();
        if(StrUtil.isBlank(userId)){
            throw new ClientException("用户ID获取失败,请登录");
        }
        return userId;
    }

    /**
     * 获取joinPoint拦截方法的参数的sha512
     */
    private String calcArgsSha512Hex(ProceedingJoinPoint joinPoint){
        return DigestUtil.sha512Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }
}
