package org.yeyr2.as12306.userService.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ServiceHandlerEnum {
    checkNotNull(0),
    checkUsernameUnique(1),
    checkManyDeletion(2);

    int order;

    ServiceHandlerEnum(int order) {
        this.order = order;
    }
}
