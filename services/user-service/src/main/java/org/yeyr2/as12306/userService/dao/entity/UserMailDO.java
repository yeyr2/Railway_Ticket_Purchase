package org.yeyr2.as12306.userService.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.yeyr2.as12306.database.base.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户邮箱表实体对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user_mail")
public class UserMailDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String mail;

    /**
     * 注销时间戳
     */
    private Long deletionTime;
}
