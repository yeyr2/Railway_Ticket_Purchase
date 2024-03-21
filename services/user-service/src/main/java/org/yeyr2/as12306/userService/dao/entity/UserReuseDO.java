package org.yeyr2.as12306.userService.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.yeyr2.as12306.database.base.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户名复用表实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("user_reuse")
public class UserReuseDO extends BaseDO {
    /**
     *  用户名
     */
    private String username;
}
