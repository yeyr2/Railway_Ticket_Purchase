package org.yeyr2.as12306.ticketService.service.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yeyr2.as12306.ticketService.common.constant.As12306Constant;
import org.yeyr2.as12306.ticketService.common.enums.SeatStatusEnum;
import org.yeyr2.as12306.ticketService.common.enums.TrainTypeEnum;
import org.yeyr2.as12306.ticketService.common.enums.VehicleTypeEnum;
import org.yeyr2.as12306.ticketService.dao.entity.SeatDO;
import org.yeyr2.as12306.ticketService.dao.entity.TrainDO;
import org.yeyr2.as12306.ticketService.dao.mapper.SeatMapper;
import org.yeyr2.as12306.ticketService.dao.mapper.TrainMapper;
import org.yeyr2.as12306.ticketService.dto.domain.RouteDTO;
import org.yeyr2.as12306.ticketService.service.TrainStationService;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;
import org.yeyr2.as12306.ticketService.common.constant.RedisKeyConstant;
import org.yeyr2.as12306.ticketService.common.enums.VehicleSeatTypeEnum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_SAFE_LOAD_SEAT_MARGIN_GET,keySuffix));
        lock.lock();
        try{
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            // 获取该车次该航线对应seatType的座位数量
            Object quantityObj = stringRedisTemplate.opsForHash().get(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
            if(CacheUtil.isNullOrBlank(quantityObj)){
                TrainDO trainDO = distributedCache.safeGet(
                        RedisKeyConstant.TRAIN_INFO + trainId,
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
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.BUSINESS_CLASS.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.BUSINESS_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.FIRST_CLASS.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.FIRST_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.SECOND_CLASS.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.SECOND_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                    String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                    trainStationRemoteTicketMap.put(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                                }
                            }
                            case Bullet_Train -> {
                                for (RouteDTO each : routeDTOS) {
                                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.SECOND_CLASS_CABIN_SEAT.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.SECOND_CLASS_CABIN_SEAT.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.FIRST_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.FIRST_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.SECOND_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.SECOND_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.NO_SEAT_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.NO_SEAT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                    trainStationRemoteTicketMap.put(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                                }
                            }
                            case Regular_Train -> {
                                for (RouteDTO each : routeDTOS) {
                                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.SOFT_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.SOFT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.HARD_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.HARD_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.HARD_SEAT.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.HARD_SEAT.getCode(), each.getStartStation(), each.getEndStation()));
                                    trainStationRemainingTicket.put(String.valueOf(VehicleSeatTypeEnum.NO_SEAT_SLEEPER.getCode()),
                                            selectSeatMargin(trainId, VehicleSeatTypeEnum.NO_SEAT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                    String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                    trainStationRemoteTicketMap.put(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                                }
                            }
                        }
                    }
                }else{
                    // 全部座位为0
                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                    VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType())
                            .forEach(each -> trainStationRemainingTicket.put(String.valueOf(each), "0"));
                    trainStationRemoteTicketMap.put(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix, trainStationRemainingTicket);
                }
                // TODO: LUA 脚本执行
                trainStationRemoteTicketMap.forEach((cacheKey, cacheMap) -> stringRedisTemplate.opsForHash().putAll(cacheKey, cacheMap));
            }
        }finally {
            lock.unlock();
        }
        return Optional.ofNullable(trainStationRemoteTicketMap.get(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix)).orElse(new LinkedHashMap<>());
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
