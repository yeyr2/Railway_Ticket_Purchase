package org.yeyr2.as12306.userService.dto.req;

import lombok.Data;

/**
 * 乘车人移除请求参数
 */
@Data
public class PassengerRemoveReqDTO extends UserReqDTO{

    /**
     * 乘车人id
     */
    private String id;
}
