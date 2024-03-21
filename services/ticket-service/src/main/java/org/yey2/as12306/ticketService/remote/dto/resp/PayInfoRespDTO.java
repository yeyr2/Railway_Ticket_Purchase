package org.yey2.as12306.ticketService.remote.dto.resp;

import lombok.Data;

import java.util.Date;

/**
 * 支付单详情信息返回参数
 */
@Data
public class PayInfoRespDTO {
    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 支付总金额
     */
    private Integer totalAmount;

    /**
     * 支付状态
     */
    private Integer status;

    /**
     * 支付时间
     */
    private Date gmtPayment;
}
