package org.yeyr2.as12306.ticketService.mq.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付结果回调购票服务事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayResultCallbackTicketEvent {

    /**
     * id
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     *
     */
    private String outOrderSn;

    private Integer channel;

    private String tradeType;

    private String subject;

    private String tradeNo;

    private String orderRequestId;

    private BigDecimal totalAmount;

    /**
     * 付款时间
     */
    private Date gmtPayment;

    /**
     * 支付金额
     */
    private BigDecimal payAmount;

    /**
     * 支付状态
     */
    private String status;

}
