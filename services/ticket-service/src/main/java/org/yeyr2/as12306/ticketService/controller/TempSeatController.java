package org.yeyr2.as12306.ticketService.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeyr2.as12306.ticketService.common.enums.SeatStatusEnum;
import org.yeyr2.as12306.ticketService.dao.entity.SeatDO;
import org.yeyr2.as12306.ticketService.dao.entity.TrainStationRelationDO;
import org.yeyr2.as12306.ticketService.dao.mapper.SeatMapper;
import org.yeyr2.as12306.ticketService.dao.mapper.TrainStationRelationMapper;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.common.toolkit.ThreadUtil;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.ticketService.common.constant.RedisKeyConstant;
import org.yeyr2.as12306.web.Results;

import java.util.List;

/**
 * 联调临时解决方案
 */
@RestController
@RequiredArgsConstructor
public class TempSeatController {

    private final SeatMapper seatMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    /**
     * 座位重置
     */
    @PostMapping("/api/ticket-service/temp/seat/reset")
    public Result<Void> purchaseTickets(@RequestParam String trainId) {
        SeatDO seatDO = new SeatDO();
        seatDO.setSeatStatus(SeatStatusEnum.AVAILABLE.getCode());
        seatMapper.update(seatDO, Wrappers.lambdaUpdate(SeatDO.class).eq(SeatDO::getTrainId, trainId));
        ThreadUtil.sleep(5000);
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper.selectList(Wrappers.lambdaQuery(TrainStationRelationDO.class)
                .eq(TrainStationRelationDO::getTrainId, trainId));
        for (TrainStationRelationDO each : trainStationRelationDOList) {
            String keySuffix = StrUtil.join("_", each.getTrainId(), each.getDeparture(), each.getArrival());
            stringRedisTemplate.delete(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix);
        }
        stringRedisTemplate.delete(RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET + trainId);
        return Results.success();
    }
}
