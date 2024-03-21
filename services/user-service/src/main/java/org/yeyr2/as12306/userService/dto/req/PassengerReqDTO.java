package org.yeyr2.as12306.userService.dto.req;

import lombok.Data;

/**
 * 乘车人添加与修改的请求参数
 */
@Data
public class PassengerReqDTO extends UserReqDTO{

    /**
     *  乘车人id
     */
    private String id;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号码
     */
    private String idCard;

    /**
     * 优惠类型
     */
    private Integer discountType;

    /**
     * 手机号
     */
    private String phone;
}
