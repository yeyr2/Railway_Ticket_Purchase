package org.yeyr2.as12306.userService.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.yeyr2.as12306.userService.dao.entity.UserMailDO;

/**
 * 用户邮箱表持久层
 */
public interface UserMailMapper extends BaseMapper<UserMailDO> {

    /**
     * 注销用户
     * @param userMailDO 注销用户参数
     */
    void deletionUser(UserMailDO userMailDO);
}
