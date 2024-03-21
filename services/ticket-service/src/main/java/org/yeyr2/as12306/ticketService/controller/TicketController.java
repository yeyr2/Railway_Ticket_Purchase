package org.yeyr2.as12306.ticketService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yeyr2.as12306.ticketService.dto.req.CancelTicketOrderReqDTO;
import org.yeyr2.as12306.ticketService.dto.req.PurchaseTicketReqDTO;
import org.yeyr2.as12306.ticketService.dto.req.RefundTicketReqDTO;
import org.yeyr2.as12306.ticketService.dto.req.TicketPageQueryReqDTO;
import org.yeyr2.as12306.ticketService.dto.resp.RefundTicketRespDTO;
import org.yeyr2.as12306.ticketService.dto.resp.TicketPageQueryRespDTO;
import org.yeyr2.as12306.ticketService.dto.resp.TicketPurchaseRespDTO;
import org.yeyr2.as12306.ticketService.remote.dto.resp.PayInfoRespDTO;
import org.yeyr2.as12306.ticketService.service.TicketService;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;
import org.yeyr2.as12306.idempotent.enums.IdempotentSceneEnum;
import org.yeyr2.as12306.idempotent.enums.IdempotentTypeEnum;
import org.yeyr2.as12306.log.annotation.ILog;
import org.yeyr2.as12306.web.Results;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;


    /**
     * 根据条件查询车票
     */
    @GetMapping("/api/ticket-service/ticket/query")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO req){
        return Results.success(ticketService.pageListTicketQueryV2(req));
    }

    /**
     * 购买车票
     */
    @ILog
    @Idempotent(
            uniqueKeyPrefix = "as12306-ticket:lock_purchase-tickets:",
            key = "T(org.yeyr2.as12306.base.ApplicationContextHolder).getBean('environment').getProperty('unique-name', '')"
            + "+'_'+" + "T(org.yeyr2.as12306.bizs.user.core.UserContext).getUsername()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI,
            type = IdempotentTypeEnum.SPEL
    )
    @PostMapping("/api/ticket-service/ticket/purchase/v1")
    public Result<TicketPurchaseRespDTO> purchaseRespDTOResult(@RequestBody PurchaseTicketReqDTO req){
        return Results.success(ticketService.purchaseTicketsV1(req));
    }

    /**
     * 购买车票v2
     */
    @ILog
    @Idempotent(
            uniqueKeyPrefix = "index12306-ticket:lock_purchase-tickets:",
            key = "T(org.opengoofy.index12306.framework.starter.bases.ApplicationContextHolder).getBean('environment').getProperty('unique-name', '')"
                    + "+'_'+"
                    + "T(org.opengoofy.index12306.frameworks.starter.user.core.UserContext).getUsername()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI,
            type = IdempotentTypeEnum.SPEL
    )
    @PostMapping("/api/ticket-service/ticket/purchase/v2")
    public Result<TicketPurchaseRespDTO> purchaseTicketsV2(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV2(requestParam));
    }

    /**
     * 取消车票订单
     */
    @ILog
    @PostMapping("/api/ticket-service/ticket/cancel")
    public Result<Void> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO req){
        ticketService.cancelTicketOrder(req);
        return Results.success();
    }

    /**
     * 支付单详情查询
     */
    @GetMapping("/api/ticket-service/ticket/pay/query")
    public Result<PayInfoRespDTO> getPayInfo(@RequestParam("orderSn") String orderSn){
        return Results.success(ticketService.getPayInfo(orderSn));
    }

    /**
     * 公共退款接口
     */
    @PostMapping("/api/ticket-service/ticket/refund")
    public Result<RefundTicketRespDTO> commonTicketRefund(@RequestBody RefundTicketReqDTO req){
        return Results.success(ticketService.commonTicketRefund(req));
    }
}
