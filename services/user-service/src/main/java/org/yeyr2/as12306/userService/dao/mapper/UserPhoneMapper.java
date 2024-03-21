package org.yeyr2.as12306.userService.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.yeyr2.as12306.userService.dao.entity.UserPhoneDO;

/**
 * 用户手机号持久层
 */
public interface UserPhoneMapper extends BaseMapper<UserPhoneDO> {

    /**
     * 注销用户
     *
     * @param userPhoneDO 注销用户入参
     */
    void deletionUser(UserPhoneDO userPhoneDO);
}
