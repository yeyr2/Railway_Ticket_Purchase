package org.yeyr2.as12306.idempotent.core.token;

import cn.hutool.core.util.StrUtil;
import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.convention.errorcode.BaseErrorCode;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.idempotent.config.IdempotentProperties;
import org.yeyr2.as12306.idempotent.core.AbstractIdempotentExecuteHandler;
import org.yeyr2.as12306.idempotent.core.IdempotentParamWrapper;

import java.util.Optional;
import java.util.UUID;

// 基于token验证请求幂等性,通常应用于RestAPI方法
@RequiredArgsConstructor
public final class IdempotentTokenExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentTokenService {

    private final DistributedCache distributedCache;
    private final IdempotentProperties idempotentProperties;
    private static final String TOKEN_KEY = "token";
    private static final String TOKEN_PREFIX_KEY = "idempotent:token:";
    private static final long TOKEN_EXPIRED_TIME = 6000;
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        return new IdempotentParamWrapper();
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 获取传入的token并验证删除
        HttpServletRequest request = ((ServletRequestAttributes)(RequestContextHolder.currentRequestAttributes()))
                .getRequest();
        String token = request.getHeader(TOKEN_KEY);
        if(StrUtil.isBlank(token)){
            token = request.getParameter(TOKEN_KEY);
            if(StrUtil.isBlank(token)){
                throw new ClientException(BaseErrorCode.IDEMPOTENT_TOKEN_NULL_ERROR);
            }
        }
        Boolean tokenDelFlag = distributedCache.delete(token);
        if(!tokenDelFlag){
            String errMsg = StrUtil.isNotBlank(wrapper.getIdempotent().message())
                    ? wrapper.getIdempotent().message()
                    : BaseErrorCode.IDEMPOTENT_TOKEN_DELETE_ERROR.message();
            throw new ClientException(errMsg,BaseErrorCode.IDEMPOTENT_TOKEN_DELETE_ERROR);
        }
    }

    @Override
    public void exceptionProcessing() {
        super.exceptionProcessing();
    }

    @Override
    public void postProcessing() {
        super.postProcessing();
    }

    @Override
    public String createToken() {
        String token = Optional.ofNullable(Strings.emptyToNull(idempotentProperties.getPrefix()))
                .orElse(TOKEN_PREFIX_KEY) + UUID.randomUUID();
        distributedCache.put(token,"",Optional.ofNullable(idempotentProperties.getTimeout())
                .orElse(TOKEN_EXPIRED_TIME));
        return token;
    }
}
