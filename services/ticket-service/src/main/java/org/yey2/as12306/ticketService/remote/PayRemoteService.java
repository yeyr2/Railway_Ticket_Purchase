package org.yey2.as12306.ticketService.remote;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.yey2.as12306.ticketService.remote.dto.req.RefundReqDTO;
import org.yey2.as12306.ticketService.remote.dto.resp.PayInfoRespDTO;
import org.yey2.as12306.ticketService.remote.dto.resp.RefundRespDTO;
import org.yeyr2.as12306.convention.result.Result;

/**
 * 支付单远程调用服务
 */
@FeignClient(value = "as12306-pay${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface PayRemoteService {

    /**
     * 支付单详情查询 - 跟据订单号查询支付单详情
     */
    @GetMapping("/api/pay-service/pay/query/order-sn")
    Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn);

    /**
     * 公共退款接口
     */
    @PostMapping("/api/pay-service/common/refund")
    Result<RefundRespDTO> commonRefund(@RequestBody RefundReqDTO requestParam);

}
