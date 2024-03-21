/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yeyr2.as12306.payService.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.yeyr2.as12306.payService.common.enums.PayChannelEnum;
import org.yeyr2.as12306.payService.common.enums.TradeStatusEnum;
import org.yeyr2.as12306.payService.dto.PayCallbackReqDTO;
import org.yeyr2.as12306.payService.dto.base.AliPayCallbackRequest;
import org.yeyr2.as12306.payService.dto.base.PayCallbackRequest;
import org.yeyr2.as12306.payService.handler.base.AbstractPayCallbackHandler;
import org.yeyr2.as12306.payService.service.PayService;
import org.springframework.stereotype.Service;
import org.yeyr2.as12306.designpattern.strategy.AbstractExecuteStrategy;

/**
 * 阿里支付回调组件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class AliPayCallbackHandler extends AbstractPayCallbackHandler implements AbstractExecuteStrategy<PayCallbackRequest, Void> {

    private final PayService payService;

    @Override
    public void callback(PayCallbackRequest payCallbackRequest) {
        AliPayCallbackRequest aliPayCallBackRequest = payCallbackRequest.getAliPayCallBackRequest();
        PayCallbackReqDTO payCallbackRequestParam = PayCallbackReqDTO.builder()
                .status(TradeStatusEnum.queryActualTradeStatusCode(aliPayCallBackRequest.getTradeStatus()))
                .payAmount(aliPayCallBackRequest.getBuyerPayAmount())
                .tradeNo(aliPayCallBackRequest.getTradeNo())
                .gmtPayment(aliPayCallBackRequest.getGmtPayment())
                .orderSn(aliPayCallBackRequest.getOrderRequestId())
                .build();
        payService.callbackPay(payCallbackRequestParam);
    }

    @Override
    public String mark() {
        return PayChannelEnum.ALI_PAY.name();
    }

    public void execute(PayCallbackRequest requestParam) {
        callback(requestParam);
    }
}
