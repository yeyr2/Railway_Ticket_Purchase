package org.yey2.as12306.ticketService.service.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yey2.as12306.ticketService.common.constant.As12306Constant;
import org.yey2.as12306.ticketService.common.enums.SeatStatusEnum;
import org.yey2.as12306.ticketService.common.enums.TrainTypeEnum;
import org.yey2.as12306.ticketService.common.enums.VehicleTypeEnum;
import org.yey2.as12306.ticketService.dao.entity.SeatDO;
import org.yey2.as12306.ticketService.dao.entity.TrainDO;
import org.yey2.as12306.ticketService.dao.mapper.SeatMapper;
import org.yey2.as12306.ticketService.dao.mapper.TrainMapper;
import org.yey2.as12306.ticketService.dto.domain.RouteDTO;
import org.yey2.as12306.ticketService.service.TrainStationService;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.yey2.as12306.ticketService.common.constant.RedisKeyConstant.*;
import static org.yey2.as12306.ticketService.common.enums.VehicleSeatTypeEnum.*;

/**
 * 座位余量缓存加载
 */
@Component
@AllArgsConstructor
public class SeatMarginCacheLoader {
    private TrainMapper trainMapper;
    private SeatMapper seatMapper;
    private DistributedCache distributedCache;
    private RedissonClient redissonClient;
    private TrainStationService trainStationService;

    /**
     *  检测该车次该航线对应seatType的座位数量,若不存在,则获取所有的未售座位数量并存入redis,否则查询redis中数量
     */
    public Map<String,String> load(String trainId,String seatType,String departure,String arrival){
        Map<String,Map<String,String>> trainStationRemoteTicketMap = new LinkedHashMap<>();
        String keySuffix = CacheUtil.buildKey(trainId,departure,arrival);
        RLock lock = redissonClient.getLock(String.format(LOCK_SAFE_LOAD_SEAT_MARGIN_GET,keySuffix));
        lock.lock();
        try{
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            // 获取该车次该航线对应seatType的座位数量
            Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
            if(CacheUtil.isNullOrBlank(quantityObj)){
                TrainDO trainDO = distributedCache.safeGet(
                        TRAIN_INFO + trainId,
                        TrainDO.class,
                        () -> trainMapper.selectById(trainId),
                        As12306Constant.ADVANCE_TICKET_DAY,
                        TimeUnit.DAYS
                );
                List<RouteDTO> routeDTOS = trainStationService.listTrainStationRoute(trainId,trainDO.getStartStation(), trainDO.getEndStation());
                if(CacheUtil.isNullOrBlank(routeDTOS)){
                    TrainTypeEnum trainType = TrainTypeEnum.transform(trainDO.getTrainType());
                    if(trainType != null){ // 类型正常
                        // 获取对应类型载具的各种座位的数量
                        switch (trainType){
                            case High_Speed_Rail -> {
                                for (RouteDTO each : routeDTOS) {
                                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                    trainStationRemainingTicket.put(String.valueOf(BUSINESS_CLASS.getCode()),
                                            selectSeatMargin(trainId, BUSINESS_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(FIRST_CLASS.getCode()),
                                            selectSeatMargin(trainId, FIRST_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(SECOND_CLASS.getCode()),
                                            selectSeatMargin(trainId, SECOND_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                    String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                    trainStationRemoteTicketMap.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                                }
                            }
                            case Bullet_Train -> {
                                for (RouteDTO each : routeDTOS) {
                                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                    trainStationRemainingTicket.put(String.valueOf(SECOND_CLASS_CABIN_SEAT.getCode()),
                                            selectSeatMargin(trainId, SECOND_CLASS_CABIN_SEAT.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(FIRST_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, FIRST_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(SECOND_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, SECOND_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(NO_SEAT_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, NO_SEAT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                    trainStationRemoteTicketMap.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                                }
                            }
                            case Regular_Train -> {
                                for (RouteDTO each : routeDTOS) {
                                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                    trainStationRemainingTicket.put(String.valueOf(SOFT_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, SOFT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(HARD_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, HARD_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(HARD_SEAT.getCode()),
                                            selectSeatMargin(trainId, HARD_SEAT.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(NO_SEAT_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, NO_SEAT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                    trainStationRemoteTicketMap.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                                }
                            }
                        }
                    }
                }else{
                    // 全部座位为0
                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                    VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType())
                            .forEach(each -> trainStationRemainingTicket.put(String.valueOf(each), "0"));
                    trainStationRemoteTicketMap.put(TRAIN_STATION_REMAINING_TICKET + keySuffix, trainStationRemainingTicket);
                }
                // TODO: LUA 脚本执行
                trainStationRemoteTicketMap.forEach((cacheKey, cacheMap) -> stringRedisTemplate.opsForHash().putAll(cacheKey, cacheMap));
            }
        }finally {
            lock.unlock();
        }
        return Optional.ofNullable(trainStationRemoteTicketMap.get(TRAIN_STATION_REMAINING_TICKET + keySuffix)).orElse(new LinkedHashMap<>());
    }

    /**
     * 获取未售座位数量
     */
    private String selectSeatMargin(String trainId,Integer type,String departure,String arrival){
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, type)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival);
        return Optional.ofNullable(seatMapper.selectCount(queryWrapper))
                .map(String::valueOf)
                .orElse("0");
    }
}
