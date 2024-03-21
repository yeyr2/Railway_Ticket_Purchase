package org.yeyr2.as12306.ticketService.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 座位状态枚举
 */
@RequiredArgsConstructor
@Getter
public enum SeatStatusEnum {

    /**
     * 可售
     */
    AVAILABLE(0),

    /**
     * 锁定
     */
    LOCKED(1),

    /**
     * 已售
     */
    SOLD(2);

    private final Integer code;
}
