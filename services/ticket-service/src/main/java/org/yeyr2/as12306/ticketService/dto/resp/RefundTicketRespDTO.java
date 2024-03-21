package org.yeyr2.as12306.ticketService.dto.resp;

import lombok.Data;
import org.yeyr2.as12306.ticketService.dto.domain.PurchaseTicketPassengerDetailDTO;

import java.util.List;

/**
 * 车票退款返回详情实体
 */
@Data
public class RefundTicketRespDTO {
    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 退款类型 0 部分退款 1 全部退款
     */
    private Integer type;

    /**
     * 乘车人
     */
    private List<PurchaseTicketPassengerDetailDTO> passengers;

    /**
     * 退款金额
     */
    private String refundAmount;
}
