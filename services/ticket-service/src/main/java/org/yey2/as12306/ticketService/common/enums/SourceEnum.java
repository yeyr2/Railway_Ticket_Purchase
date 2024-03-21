package org.yey2.as12306.ticketService.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 购票来源
 */
@RequiredArgsConstructor
@Getter
public enum SourceEnum {

    /**
     * 互联网购票
     */
    INTERNET(0),

    /**
     * 线下窗口购票
     */
    OFFLINE(1);

    private final Integer code;
}
