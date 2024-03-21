package org.yey2.as12306.ticketService.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 车票状态枚举
 */
@RequiredArgsConstructor
@Getter
public enum TicketStatusEnum {

    /**
     * 未支付
     */
    UNPAID(0),

    /**
     * 已支付
     */
    PAID(1),

    /**
     * 已进站
     */
    BOARDED(2),

    /**
     * 改签
     */
    CHANGED(3),

    /**
     * 退票
     */
    REFUNDED(4),

    /**
     * 已取消
     */
    CLOSED(5);

    private final Integer code;
}
