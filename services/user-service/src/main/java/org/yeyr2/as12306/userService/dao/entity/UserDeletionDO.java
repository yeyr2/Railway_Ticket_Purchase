package org.yeyr2.as12306.userService.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.yeyr2.as12306.database.base.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注销实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_deletion")
public class UserDeletionDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     *  证件号
     */
    private String idCard;
}
