package org.yey2.as12306.ticketService.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yey2.as12306.ticketService.common.constant.As12306Constant;
import org.yey2.as12306.ticketService.dao.entity.TrainDO;
import org.yey2.as12306.ticketService.dao.entity.TrainStationRelationDO;
import org.yey2.as12306.ticketService.dao.mapper.TrainStationRelationMapper;
import org.yey2.as12306.ticketService.job.base.AbstractTrainStationJobHandlerTemplate;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.hutool.core.date.DatePattern.NORM_DATETIME_MINUTE_FORMAT;
import static org.yey2.as12306.ticketService.common.constant.RedisKeyConstant.TRAIN_STATION_DETAIL;

/**
 * 站点详细信息定时任务
 */
@RestController
@RequiredArgsConstructor
public class TrainStationDetailJobHandler extends AbstractTrainStationJobHandlerTemplate {

    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    @XxlJob(value = "trainStationDetailJobHandler")
    @GetMapping("/api/ticket-service/train-station-detail/job/cache-init/execute")
    @Override
    public void execute() {
        super.execute();
    }

    /**
     * 查询列车对应将车次信息并写入redis
     * @param trainDOPageRecords 列车信息分页记录
     */
    @Override
    protected void actualExecute(List<TrainDO> trainDOPageRecords) {
        for (TrainDO each : trainDOPageRecords) {
            LambdaQueryWrapper<TrainStationRelationDO> relationDOLambdaQueryWrapper =
                    Wrappers.lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, each.getId());
            List<TrainStationRelationDO> trainStationRelations = trainStationRelationMapper.selectList(relationDOLambdaQueryWrapper);
            if(CollUtil.isEmpty(trainStationRelations)){
                return;
            }
            for (TrainStationRelationDO item : trainStationRelations) {
                Map<String,String> actualCacheHashValue = MapUtil.builder("trainNumber", each.getTrainNumber())
                        .put("departureFlag", BooleanUtil.toStringTrueFalse(item.getDepartureFlag()))
                        .put("arrivalFlag", BooleanUtil.toStringTrueFalse(item.getArrivalFlag()))
                        .put("departureTime", DateUtil.format(item.getDepartureTime(), "HH:mm"))
                        .put("arrivalTime", DateUtil.format(item.getArrivalTime(), "HH:mm"))
                        .put("saleTime", DateUtil.format(each.getSaleTime(), NORM_DATETIME_MINUTE_FORMAT))
                        .put("trainTag", each.getTrainTag())
                        .build();
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                String buildCacheKey = TRAIN_STATION_DETAIL + CacheUtil.buildKey(String.valueOf(each.getId()), item.getDeparture(), item.getArrival());
                stringRedisTemplate.opsForHash().putAll(buildCacheKey, actualCacheHashValue);
                stringRedisTemplate.expire(buildCacheKey, As12306Constant.ADVANCE_TICKET_DAY, TimeUnit.DAYS);
            }
        }
    }

}
