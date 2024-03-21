package org.yeyr2.as12306.idempotent.core.token;

import org.yeyr2.as12306.idempotent.core.IdempotentExecuteHandler;

// token实现幂等接口
public interface IdempotentTokenService extends IdempotentExecuteHandler {
    // 创建幂等验证token
    String createToken();
}
