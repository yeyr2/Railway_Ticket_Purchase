package org.yeyr2.as12306.userService.service.handler.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.userService.dto.req.UserRegisterReqDTO;
import org.yeyr2.as12306.userService.service.UserService;

import static org.yeyr2.as12306.userService.common.enums.ServiceHandlerEnum.checkManyDeletion;

/**
 * 用户注册近安插证件号是否多次注册
 */

@Component
@RequiredArgsConstructor
public class UserRegisterCheckDeletionCainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    private final UserService userService;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        Long userDeletionNum = userService.queryUserDeletionNum(requestParam.getIdType(), requestParam.getIdCard());
        if (userDeletionNum >= 5) {
            throw new ClientException("证件号多次注销账号已被加入黑名单");
        }
    }

    @Override
    public int getOrder() {
        return checkManyDeletion.getOrder();
    }
}
