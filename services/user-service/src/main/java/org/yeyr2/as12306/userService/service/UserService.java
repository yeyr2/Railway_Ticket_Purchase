package org.yeyr2.as12306.userService.service;

import jakarta.validation.constraints.NotEmpty;
import org.yeyr2.as12306.userService.dto.req.UserUpdateReqDTO;
import org.yeyr2.as12306.userService.dto.resp.UserQueryActualRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserQueryRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserRespDTO;

/**
 * 用户信息接口层
 */
public interface UserService {
    /**
     * 根据用户id查询用户信息
     * @param userId 用户id
     * @param isActual 是否需要真实信息(是否脱敏)
     * @return 用户信息
     */
    UserRespDTO queryUserByUserId(@NotEmpty String userId,@NotEmpty Boolean isActual);

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @param isActual 是否需要真实信息(是否脱敏)
     * @return 用户信息
     */
    UserRespDTO queryUserByUsername(@NotEmpty String username,@NotEmpty Boolean isActual);

    /**
     * 根据证件类型和证件号查询注销次数
     * @param idType 证件类型
     * @param idCard 证件号
     * @return 注销次数
     */
    Long queryUserDeletionNum(Integer idType,String idCard);

    /**
     * 根据用户id修改用户信息
     * @param requestParam 用户信息参数
     * @return 修改结果
     */
    void update(UserUpdateReqDTO requestParam);
}
