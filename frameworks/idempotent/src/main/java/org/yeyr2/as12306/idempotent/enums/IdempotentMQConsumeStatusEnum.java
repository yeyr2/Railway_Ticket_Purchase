package org.yeyr2.as12306.idempotent.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

// 幂等MQ消费状态枚举
@Getter
@RequiredArgsConstructor
public enum IdempotentMQConsumeStatusEnum {
    CONSUMING("0"),
    CONSUMED("1");

    private final String code;

    // 如果消费状态是消费中,则返回失败
    public static boolean isError(String consumeStatus){
        return Objects.equals(CONSUMING.code,consumeStatus);
    }
}
