package org.yeyr2.as12306.ticketService.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yeyr2.as12306.ticketService.common.constant.TicketRocketMQConstant;
import org.yeyr2.as12306.ticketService.dto.domain.RouteDTO;
import org.yeyr2.as12306.ticketService.dto.req.CancelTicketOrderReqDTO;
import org.yeyr2.as12306.ticketService.mq.domain.MessageWrapper;
import org.yeyr2.as12306.ticketService.mq.event.DelayCloseOrderEvent;
import org.yeyr2.as12306.ticketService.remote.TicketOrderRemoteService;
import org.yeyr2.as12306.ticketService.remote.dto.resp.TicketOrderDetailRespDTO;
import org.yeyr2.as12306.ticketService.remote.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.yeyr2.as12306.ticketService.service.SeatService;
import org.yeyr2.as12306.ticketService.service.TrainStationService;
import org.yeyr2.as12306.ticketService.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.yeyr2.as12306.ticketService.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;
import org.yeyr2.as12306.common.toolkit.BeanUtil;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.ticketService.common.constant.RedisKeyConstant;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 延迟关闭订单消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.ORDER_DELAY_CLOSE_TOPIC_KEY,
        selectorExpression = TicketRocketMQConstant.ORDER_DELAY_CLOSE_TAG_KEY,
        consumerGroup = TicketRocketMQConstant.TICKET_DELAY_CLOSE_CG_KEY
)
public class DelayCloseOrderConsumer implements RocketMQListener<MessageWrapper<DelayCloseOrderEvent>> {

    private final SeatService seatService;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;

    @Value("${ticket.availability.cache-update.type}")
    private String ticketAvailabilityCacheUpdateType;

    @Override
    public void onMessage(MessageWrapper<DelayCloseOrderEvent> delayCloseOrderEventMessageWrapper) {
        log.info("[延迟关闭订单] 开始消费：{}", JSON.toJSONString(delayCloseOrderEventMessageWrapper));
        DelayCloseOrderEvent delayCloseOrderEvent = delayCloseOrderEventMessageWrapper.getMessage();
        String orderSn = delayCloseOrderEvent.getOrderSn();
        Result<Boolean> closedTicketOrder;
        try{
            closedTicketOrder = ticketOrderRemoteService.closeTickOrder(new CancelTicketOrderReqDTO(orderSn));
        }catch (Throwable ex){
            log.error("[延迟关闭订单] 订单号：{} 远程调用订单服务失败", orderSn, ex);
            throw ex;
        }
        if(closedTicketOrder.isSuccess() && !StrUtil.equals(ticketAvailabilityCacheUpdateType,"binlog")){
            if(!closedTicketOrder.getData()){
                log.info("[延迟关闭订单] 订单号：{} 用户已支付订单", orderSn);
                return;
            }
            String trainId = delayCloseOrderEvent.getTrainId();
            String departure = delayCloseOrderEvent.getDeparture();
            String arrival = delayCloseOrderEvent.getArrival();
            List<TrainPurchaseTicketRespDTO> trainPurchaseTickets = delayCloseOrderEvent.getTrainPurchaseTicketResults();
            try{
                seatService.unlock(trainId,departure,arrival,trainPurchaseTickets);
            }catch (Throwable ex){
                log.error("[延迟关闭订单] 订单号：{} 回滚列车DB座位状态失败", orderSn, ex);
                throw ex;
            }
            try{
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                Map<Integer,List<TrainPurchaseTicketRespDTO>> seatTypeMap = trainPurchaseTickets.stream()
                        .collect(Collectors.groupingBy(TrainPurchaseTicketRespDTO::getSeatType));
                List<RouteDTO> routes = trainStationService.listTakeoutTrainStationRoute(trainId,departure,arrival);
                for (RouteDTO each : routes) {
                    String keySuffix = CacheUtil.buildKey(trainId,each.getStartStation(),each.getEndStation());
                    seatTypeMap.forEach((seatType,trainPurchaseTicketRespDTOList) -> stringRedisTemplate.opsForHash()
                                .increment(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix,String.valueOf(seatType),trainPurchaseTicketRespDTOList.size())
                    );
                }
                TicketOrderDetailRespDTO ticketOrderDetail = BeanUtil.convert(delayCloseOrderEvent,TicketOrderDetailRespDTO.class);
                ticketOrderDetail.setPassengerDetails(BeanUtil.convert(delayCloseOrderEvent.getTrainPurchaseTicketResults(), TicketOrderPassengerDetailRespDTO.class));
                ticketAvailabilityTokenBucket.rollbackInBucket(ticketOrderDetail);
            }catch (Throwable ex){
                log.error("[延迟关闭订单] 订单号：{} 回滚列车Cache余票失败", orderSn, ex);
                throw ex;
            }
        }
    }
}
