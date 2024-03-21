package org.yeyr2.as12306.ticketService.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yeyr2.as12306.ticketService.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;

import java.util.List;

/**
 * 延迟关闭订单事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayCloseOrderEvent {
    /**
     * 车次 ID
     */
    private String trainId;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 乘车人购票信息
     */
    private List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults;
}
