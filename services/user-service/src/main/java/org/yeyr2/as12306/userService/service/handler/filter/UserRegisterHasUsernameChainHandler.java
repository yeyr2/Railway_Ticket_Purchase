package org.yeyr2.as12306.userService.service.handler.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.userService.common.enums.UserRegisterErrorCodeEnum;
import org.yeyr2.as12306.userService.dto.req.UserRegisterReqDTO;
import org.yeyr2.as12306.userService.service.UserLoginService;

import static org.yeyr2.as12306.userService.common.enums.ServiceHandlerEnum.checkUsernameUnique;

/**
 * 用户注册用户名唯一检验
 */
@Component
@RequiredArgsConstructor
public class UserRegisterHasUsernameChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO>{

    private UserLoginService userLoginService;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        if(!userLoginService.hasUsername(requestParam.getUsername())){
            throw new ClientException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL);
        }
    }

    @Override
    public int getOrder() {
        return checkUsernameUnique.getOrder();
    }
}
