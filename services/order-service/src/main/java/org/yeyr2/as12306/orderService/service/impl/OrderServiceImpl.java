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

package org.yeyr2.as12306.orderService.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.yeyr2.as12306.orderService.common.enums.OrderCanalErrorCodeEnum;
import org.yeyr2.as12306.orderService.common.enums.OrderItemStatusEnum;
import org.yeyr2.as12306.orderService.common.enums.OrderStatusEnum;
import org.yeyr2.as12306.orderService.dao.entity.OrderDO;
import org.yeyr2.as12306.orderService.dao.entity.OrderItemDO;
import org.yeyr2.as12306.orderService.dao.entity.OrderItemPassengerDO;
import org.yeyr2.as12306.orderService.dao.mapper.OrderItemMapper;
import org.yeyr2.as12306.orderService.dao.mapper.OrderMapper;
import org.yeyr2.as12306.orderService.dto.domain.OrderStatusReversalDTO;
import org.yeyr2.as12306.orderService.dto.req.*;
import org.yeyr2.as12306.orderService.dto.resp.TicketOrderDetailRespDTO;
import org.yeyr2.as12306.orderService.dto.resp.TicketOrderDetailSelfRespDTO;
import org.yeyr2.as12306.orderService.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.yeyr2.as12306.orderService.mq.event.DelayCloseOrderEvent;
import org.yeyr2.as12306.orderService.mq.event.PayResultCallbackOrderEvent;
import org.yeyr2.as12306.orderService.mq.produce.DelayCloseOrderSendProduce;
import org.yeyr2.as12306.orderService.remote.UserRemoteService;
import org.yeyr2.as12306.orderService.remote.dto.UserQueryActualRespDTO;
import org.yeyr2.as12306.orderService.service.OrderItemService;
import org.yeyr2.as12306.orderService.service.OrderPassengerRelationService;
import org.yeyr2.as12306.orderService.service.OrderService;
import org.yeyr2.as12306.orderService.service.orderid.OrderIdGeneratorManager;
import org.yeyr2.as12306.common.toolkit.BeanUtil;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.convention.exception.ServiceException;
import org.yeyr2.as12306.convention.page.PageResponse;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.database.toolkit.PageUtil;
import org.yeyr2.as12306.bizs.user.core.UserContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 订单服务接口层实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderItemService orderItemService;
    private final OrderPassengerRelationService orderPassengerRelationService;
    private final RedissonClient redissonClient;
    private final DelayCloseOrderSendProduce delayCloseOrderSendProduce;
    private final UserRemoteService userRemoteService;

    @Override
    public TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        TicketOrderDetailRespDTO result = BeanUtil.convert(orderDO, TicketOrderDetailRespDTO.class);
        LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, orderSn);
        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
        result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
        return result;
    }

    @Override
    public PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, requestParam.getUserId())
                .in(OrderDO::getStatus, buildOrderStatusList(requestParam))
                .orderByDesc(OrderDO::getOrderTime);
        IPage<OrderDO> orderPage = orderMapper.selectPage(PageUtil.convert(requestParam), queryWrapper);
        return PageUtil.convert(orderPage, each -> {
            TicketOrderDetailRespDTO result = BeanUtil.convert(each, TicketOrderDetailRespDTO.class);
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn());
            List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
            result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
            return result;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String createTicketOrder(TicketOrderCreateReqDTO requestParam) {
        // 通过基因法将用户 ID 融入到订单号
        String orderSn = OrderIdGeneratorManager.generateId(requestParam.getUserId());
        OrderDO orderDO = OrderDO.builder().orderSn(orderSn)
                .orderTime(requestParam.getOrderTime())
                .departure(requestParam.getDeparture())
                .departureTime(requestParam.getDepartureTime())
                .ridingDate(requestParam.getRidingDate())
                .arrivalTime(requestParam.getArrivalTime())
                .trainNumber(requestParam.getTrainNumber())
                .arrival(requestParam.getArrival())
                .trainId(requestParam.getTrainId())
                .source(requestParam.getSource())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .username(requestParam.getUsername())
                .userId(String.valueOf(requestParam.getUserId()))
                .build();
        orderMapper.insert(orderDO);
        List<TicketOrderItemCreateReqDTO> ticketOrderItems = requestParam.getTicketOrderItems();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        List<OrderItemPassengerDO> orderPassengerRelationDOList = new ArrayList<>();
        ticketOrderItems.forEach(each -> {
            OrderItemDO orderItemDO = OrderItemDO.builder()
                    .trainId(requestParam.getTrainId())
                    .seatNumber(each.getSeatNumber())
                    .carriageNumber(each.getCarriageNumber())
                    .realName(each.getRealName())
                    .orderSn(orderSn)
                    .phone(each.getPhone())
                    .seatType(each.getSeatType())
                    .username(requestParam.getUsername()).amount(each.getAmount()).carriageNumber(each.getCarriageNumber())
                    .idCard(each.getIdCard())
                    .ticketType(each.getTicketType())
                    .idType(each.getIdType())
                    .userId(String.valueOf(requestParam.getUserId()))
                    .status(0)
                    .build();
            orderItemDOList.add(orderItemDO);
            OrderItemPassengerDO orderPassengerRelationDO = OrderItemPassengerDO.builder()
                    .idType(each.getIdType())
                    .idCard(each.getIdCard())
                    .orderSn(orderSn)
                    .build();
            orderPassengerRelationDOList.add(orderPassengerRelationDO);
        });
        orderItemService.saveBatch(orderItemDOList);
        orderPassengerRelationService.saveBatch(orderPassengerRelationDOList);
        try {
            // 发送 RocketMQ 延时消息，指定时间后取消订单
            DelayCloseOrderEvent delayCloseOrderEvent = DelayCloseOrderEvent.builder()
                    .trainId(String.valueOf(requestParam.getTrainId()))
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderSn(orderSn)
                    .trainPurchaseTicketResults(requestParam.getTicketOrderItems())
                    .build();
            // 创建订单并支付后延时关闭订单消息怎么办？详情查看：https://nageoffer.com/12306/question
            SendResult sendResult = delayCloseOrderSendProduce.sendMessage(delayCloseOrderEvent);
            if (!Objects.equals(sendResult.getSendStatus(), SendStatus.SEND_OK)) {
                throw new ServiceException("投递延迟关闭订单消息队列失败");
            }
        } catch (Throwable ex) {
            log.error("延迟关闭订单消息队列发送错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
            throw ex;
        }
        return orderSn;
    }

    @Override
    public boolean closeTickOrder(CancelTicketOrderReqDTO requestParam) {
        String orderSn = requestParam.getOrderSn();
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn)
                .select(OrderDO::getStatus);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        if (Objects.isNull(orderDO) || orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            return false;
        }
        // 原则上订单关闭和订单取消这两个方法可以复用，为了区分未来考虑到的场景，这里对方法进行拆分但复用逻辑
        return cancelTickOrder(requestParam);
    }

    @Override
    public boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam) {
        String orderSn = requestParam.getOrderSn();
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        RLock lock = redissonClient.getLock(StrBuilder.create("order:canal:order_sn_").append(orderSn).toString());
        if (!lock.tryLock()) {
            throw new ClientException(OrderCanalErrorCodeEnum.ORDER_CANAL_REPETITION_ERROR);
        }
        try {
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(OrderStatusEnum.CLOSED.getStatus());
            updateOrderDO.setOrderSn(orderSn);
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, orderSn);
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
            OrderItemDO updateOrderItemDO = new OrderItemDO();
            updateOrderItemDO.setStatus(OrderItemStatusEnum.CLOSED.getStatus());
            updateOrderItemDO.setOrderSn(orderSn);
            LambdaUpdateWrapper<OrderItemDO> updateItemWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, orderSn);
            int updateItemResult = orderItemMapper.update(updateOrderItemDO, updateItemWrapper);
            if (updateItemResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Override
    public void statusReversal(OrderStatusReversalDTO requestParam) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        RLock lock = redissonClient.getLock(StrBuilder.create("order:status-reversal:order_sn_").append(requestParam.getOrderSn()).toString());
        if (!lock.tryLock()) {
            log.warn("订单重复修改状态，状态反转请求参数：{}", JSON.toJSONString(requestParam));
        }
        try {
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(requestParam.getOrderStatus());
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
            OrderItemDO orderItemDO = new OrderItemDO();
            orderItemDO.setStatus(requestParam.getOrderItemStatus());
            LambdaUpdateWrapper<OrderItemDO> orderItemUpdateWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn());
            int orderItemUpdateResult = orderItemMapper.update(orderItemDO, orderItemUpdateWrapper);
            if (orderItemUpdateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void payCallbackOrder(PayResultCallbackOrderEvent requestParam) {
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setPayTime(requestParam.getGmtPayment());
        updateOrderDO.setPayType(requestParam.getChannel());
        LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
        if (updateResult <= 0) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }

    @Override
    public PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        Result<UserQueryActualRespDTO> userActualResp = userRemoteService.queryActualUserByUsername(UserContext.getUsername());
        LambdaQueryWrapper<OrderItemPassengerDO> queryWrapper = Wrappers.lambdaQuery(OrderItemPassengerDO.class)
                .eq(OrderItemPassengerDO::getIdCard, userActualResp.getData().getIdCard())
                .orderByDesc(OrderItemPassengerDO::getCreateTime);
        IPage<OrderItemPassengerDO> orderItemPassengerPage = orderPassengerRelationService.page(PageUtil.convert(requestParam), queryWrapper);
        return PageUtil.convert(orderItemPassengerPage, each -> {
            LambdaQueryWrapper<OrderDO> orderQueryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                    .eq(OrderDO::getOrderSn, each.getOrderSn());
            OrderDO orderDO = orderMapper.selectOne(orderQueryWrapper);
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn())
                    .eq(OrderItemDO::getIdCard, each.getIdCard());
            OrderItemDO orderItemDO = orderItemMapper.selectOne(orderItemQueryWrapper);
            TicketOrderDetailSelfRespDTO actualResult = BeanUtil.convert(orderDO, TicketOrderDetailSelfRespDTO.class);
            BeanUtil.convertIgnoreNullAndBlank(orderItemDO, actualResult);
            return actualResult;
        });
    }

    private List<Integer> buildOrderStatusList(TicketOrderPageQueryReqDTO requestParam) {
        List<Integer> result = new ArrayList<>();
        switch (requestParam.getStatusType()) {
            case 0 -> result = ListUtil.of(
                    OrderStatusEnum.PENDING_PAYMENT.getStatus()
            );
            case 1 -> result = ListUtil.of(
                    OrderStatusEnum.ALREADY_PAID.getStatus(),
                    OrderStatusEnum.PARTIAL_REFUND.getStatus(),
                    OrderStatusEnum.FULL_REFUND.getStatus()
            );
            case 2 -> result = ListUtil.of(
                    OrderStatusEnum.COMPLETED.getStatus()
            );
        }
        return result;
    }
}
