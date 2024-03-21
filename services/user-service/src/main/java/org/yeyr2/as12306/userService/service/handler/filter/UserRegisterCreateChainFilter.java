package org.yeyr2.as12306.userService.service.handler.filter;

import org.yeyr2.as12306.designpattern.chain.AbstractChainHandler;
import org.yeyr2.as12306.userService.common.enums.UserChainMarkEnum;
import org.yeyr2.as12306.userService.dto.req.UserRegisterReqDTO;

public interface UserRegisterCreateChainFilter<T extends UserRegisterReqDTO> extends AbstractChainHandler<UserRegisterReqDTO> {

    @Override
    default String mark() {
        return UserChainMarkEnum.USER_REGISTER_FILTER.name();
    }
}
