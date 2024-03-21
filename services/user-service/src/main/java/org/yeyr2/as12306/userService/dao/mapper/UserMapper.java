package org.yeyr2.as12306.userService.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.yeyr2.as12306.userService.dao.entity.UserDO;

/**
 * 用户信息持久层
 */
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 注销用户
     * @param userDO 注销用户参数
     */
    void deletionUser(UserDO userDO);
}
