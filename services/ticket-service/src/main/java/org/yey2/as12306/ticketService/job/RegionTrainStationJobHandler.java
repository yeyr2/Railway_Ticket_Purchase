package org.yey2.as12306.ticketService.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.yey2.as12306.ticketService.common.constant.As12306Constant;
import org.yey2.as12306.ticketService.common.constant.RedisKeyConstant;
import org.yey2.as12306.ticketService.dao.entity.RegionDO;
import org.yey2.as12306.ticketService.dao.entity.TrainStationRelationDO;
import org.yey2.as12306.ticketService.dao.mapper.RegionMapper;
import org.yey2.as12306.ticketService.dao.mapper.TrainStationRelationMapper;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;
import org.yeyr2.as12306.common.toolkit.EnvironmentUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 地区站点查询定时任务
 */
@RestController
@RequiredArgsConstructor
public class RegionTrainStationJobHandler extends IJobHandler {

    private final RegionMapper regionMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    // 将各站点车次的出发时间写入redis
    @XxlJob(value = "regionTrainStationJobHandler")
    @GetMapping("/api/ticket-service/region-train-station/job/cache-init/execute")
    @Override
    public void execute() {
        List<String> regionList = regionMapper.selectList(Wrappers.emptyWrapper())
                .stream()
                .map(RegionDO::getName)
                .toList();
        String requestParam = getJobRequestParam();
        String dateTime = StrUtil.isNotBlank(requestParam) ? requestParam : DateUtil.tomorrow().toDateStr();
        for(int i = 0 ; i < regionList.size() ; i++) {
            for(int j = 0 ; j < regionList.size() ; j++){
                if( i != j ){
                    String startRegion = regionList.get(i);
                    String endRegion = regionList.get(j);
                    LambdaQueryWrapper<TrainStationRelationDO> relationQueryWrapper =
                            Wrappers.lambdaQuery(TrainStationRelationDO.class)
                                    .eq(TrainStationRelationDO::getStartRegion, startRegion)
                                    .eq(TrainStationRelationDO::getEndRegion, endRegion);
                    List<TrainStationRelationDO> trainStationRelations = trainStationRelationMapper.selectList(relationQueryWrapper);
                    if(CollUtil.isEmpty(trainStationRelations)){
                        continue;
                    }
                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                    trainStationRelations.forEach(item -> {
                        String zSetKey = CacheUtil.buildKey(String.valueOf(item.getTrainId()),item.getDeparture(),item.getArrival());
                        ZSetOperations.TypedTuple<String> tuple = ZSetOperations.TypedTuple.of(zSetKey, (double) item.getDepartureTime().getTime());
                        tuples.add(tuple);
                    });
                    StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                    String buildCacheKey = RedisKeyConstant.REGION_TRAIN_STATION + CacheUtil.buildKey(startRegion,endRegion,dateTime);
                    stringRedisTemplate.opsForZSet().add(buildCacheKey,tuples);
                    stringRedisTemplate.expire(buildCacheKey, As12306Constant.ADVANCE_TICKET_DAY, TimeUnit.DAYS);
                }
            }
        }
    }

    private String getJobRequestParam() {
        return EnvironmentUtil.isDevEnvironment()
                ? ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest().getHeader("requestParam")
                : XxlJobHelper.getJobParam();
    }
}
