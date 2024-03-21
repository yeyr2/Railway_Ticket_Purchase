package org.yeyr2.as12306.idempotent.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 重复消费异常
@Getter
@RequiredArgsConstructor
public class RepeatConsumptionException extends RuntimeException {
    private final Boolean error;
}
