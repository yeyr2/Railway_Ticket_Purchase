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

package org.yeyr2.as12306.ticketService.service.handler.ticket.select;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yeyr2.as12306.ticketService.common.enums.VehicleSeatTypeEnum;
import org.yeyr2.as12306.ticketService.common.enums.VehicleTypeEnum;
import org.yeyr2.as12306.ticketService.dao.entity.TrainStationPriceDO;
import org.yeyr2.as12306.ticketService.dao.mapper.TrainStationPriceMapper;
import org.yeyr2.as12306.ticketService.dto.domain.PurchaseTicketPassengerDetailDTO;
import org.yeyr2.as12306.ticketService.dto.req.PurchaseTicketReqDTO;
import org.yeyr2.as12306.ticketService.remote.UserRemoteService;
import org.yeyr2.as12306.ticketService.remote.dto.resp.PassengerRespDTO;
import org.yeyr2.as12306.ticketService.service.SeatService;
import org.yeyr2.as12306.ticketService.service.handler.ticket.dto.SelectSeatDTO;
import org.yeyr2.as12306.ticketService.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.yeyr2.as12306.bizs.user.core.UserContext;
import org.yeyr2.as12306.convention.exception.RemoteException;
import org.yeyr2.as12306.convention.exception.ServiceException;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.designpattern.strategy.AbstractStrategyChoose;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 购票时列车座位选择器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class TrainSeatTypeSelector {

    private final SeatService seatService;
    private final UserRemoteService userRemoteService;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final ThreadPoolExecutor selectSeatThreadPoolExecutor;

    /**
     *
     */
    public List<TrainPurchaseTicketRespDTO> select(Integer trainType, PurchaseTicketReqDTO requestParam) {
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        List<TrainPurchaseTicketRespDTO> actualResult = new CopyOnWriteArrayList<>();
        if (seatTypeMap.size() > 1) {
            // 根据客户需求购买每种座位,使用异步
            List<Future<List<TrainPurchaseTicketRespDTO>>> futureResults = new ArrayList<>();
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                // 线程池参数设置
                Future<List<TrainPurchaseTicketRespDTO>> completableFuture = selectSeatThreadPoolExecutor
                        .submit(() -> distributeSeats(trainType, seatType, requestParam, passengerSeatDetails));
                futureResults.add(completableFuture);
            });
            // 并行流极端情况下有坑
            futureResults.parallelStream().forEach(completableFuture -> {
                try {
                    actualResult.addAll(completableFuture.get());
                } catch (Exception e) {
                    throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
                }
            });
        } else {
            // 只有一种座位时不使用线程
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                List<TrainPurchaseTicketRespDTO> aggregationResult = distributeSeats(trainType, seatType, requestParam, passengerSeatDetails);
                actualResult.addAll(aggregationResult);
            });
        }
        if (CollUtil.isEmpty(actualResult) || !Objects.equals(actualResult.size(), passengerDetails.size())) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
        }
        List<String> passengerIds = actualResult.stream()
                .map(TrainPurchaseTicketRespDTO::getPassengerId)
                .collect(Collectors.toList());
        // 远程调用请求获取乘客信息
        Result<List<PassengerRespDTO>> passengerRemoteResult;
        List<PassengerRespDTO> passengerRemoteResultList;
        try {
            passengerRemoteResult = userRemoteService.listPassengerQueryByIds(UserContext.getUsername(), passengerIds);
            if (!passengerRemoteResult.isSuccess() || CollUtil.isEmpty(passengerRemoteResultList = passengerRemoteResult.getData())) {
                throw new RemoteException("用户服务远程调用查询乘车人相信信息错误");
            }
        } catch (Throwable ex) {
            if (ex instanceof RemoteException) {
                log.error("用户服务远程调用查询乘车人相信信息错误，当前用户：{}，请求参数：{}", UserContext.getUsername(), passengerIds);
            } else {
                log.error("用户服务远程调用查询乘车人相信信息错误，当前用户：{}，请求参数：{}", UserContext.getUsername(), passengerIds, ex);
            }
            throw ex;
        }
        actualResult.forEach(each -> {
            String passengerId = each.getPassengerId();
            passengerRemoteResultList.stream()
                    .filter(item -> Objects.equals(item.getId(), passengerId))
                    .findFirst()
                    .ifPresent(passenger -> {
                        each.setIdCard(passenger.getIdCard());
                        each.setPhone(passenger.getPhone());
                        each.setUserType(passenger.getDiscountType());
                        each.setIdType(passenger.getIdType());
                        each.setRealName(passenger.getRealName());
                    });
            // todo:查询数据库获取座位价格,为什么要查数据库,不放到redis里(因为票价格会变?)
            LambdaQueryWrapper<TrainStationPriceDO> lambdaQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                    .eq(TrainStationPriceDO::getTrainId, requestParam.getTrainId())
                    .eq(TrainStationPriceDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationPriceDO::getArrival, requestParam.getArrival())
                    .eq(TrainStationPriceDO::getSeatType, each.getSeatType())
                    .select(TrainStationPriceDO::getPrice);
            TrainStationPriceDO trainStationPriceDO = trainStationPriceMapper.selectOne(lambdaQueryWrapper);
            each.setAmount(trainStationPriceDO.getPrice());
        });
        // 购买列车中间站点余票更新
        // todo:为什么上锁放到最后,如果在获取乘客信息时,有其他请求成功获取导致无票可用,该请求等于失败,会多查一次数据库
        seatService.lockSeat(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival(), actualResult);
        return actualResult;
    }

    private List<TrainPurchaseTicketRespDTO> distributeSeats(Integer trainType, Integer seatType, PurchaseTicketReqDTO requestParam, List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails) {
        String buildStrategyKey = VehicleTypeEnum.findNameByCode(trainType) + VehicleSeatTypeEnum.findNameByCode(seatType);
        SelectSeatDTO selectSeatDTO = SelectSeatDTO.builder()
                .seatType(seatType)
                .passengerSeatDetails(passengerSeatDetails)
                .requestParam(requestParam)
                .build();
        try {
            return abstractStrategyChoose.chooseAndExecuteResp(buildStrategyKey, selectSeatDTO);
        } catch (ServiceException ex) {
            throw new ServiceException("当前车次列车类型暂未适配，请购买G35或G39车次");
        }
    }
}
