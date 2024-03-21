package org.yey2.as12306.ticketService.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.yey2.as12306.ticketService.common.constant.As12306Constant;
import org.yey2.as12306.ticketService.common.constant.RedisKeyConstant;
import org.yey2.as12306.ticketService.common.enums.RegionStationQueryTypeEnum;
import org.yey2.as12306.ticketService.dao.entity.RegionDO;
import org.yey2.as12306.ticketService.dao.entity.StationDO;
import org.yey2.as12306.ticketService.dao.mapper.RegionMapper;
import org.yey2.as12306.ticketService.dao.mapper.StationMapper;
import org.yey2.as12306.ticketService.dto.req.RegionStationQueryReqDTO;
import org.yey2.as12306.ticketService.dto.resp.RegionStationQueryRespDTO;
import org.yey2.as12306.ticketService.dto.resp.StationQueryRespDTO;
import org.yey2.as12306.ticketService.service.RegionStationService;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.cache.core.CacheLoader;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;
import org.yeyr2.as12306.common.enums.FlagEnum;
import org.yeyr2.as12306.common.toolkit.BeanUtil;
import org.yeyr2.as12306.convention.exception.ClientException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 地区以及车站接口实现层
 */
@Service
@AllArgsConstructor
public class RegionStationImpl implements RegionStationService {
    private RegionMapper regionMapper;
    private StationMapper stationMapper;
    private DistributedCache distributedCache;
    private RedissonClient redissonClient;

    @Override
    public List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam) {
        String key;
        if(StrUtil.isNotBlank(requestParam.getName())){
            key = RedisKeyConstant.REGION_STATION + requestParam.getName();
            return safeGetRegionStation(
                    key,
                    () -> {
                        LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                                .likeRight(StationDO::getName,requestParam.getName())
                                .or()
                                .likeRight(StationDO::getSpell,requestParam.getName());
                        List<StationDO> stationDOS = stationMapper.selectList(queryWrapper);
                        return JSON.toJSONString(BeanUtil.convert(stationDOS, RegionStationQueryRespDTO.class));
                    },
                    requestParam.getName()
            );
        }
        key = RedisKeyConstant.REGION_STATION + requestParam.getQueryType();
        LambdaQueryWrapper<RegionDO> queryWrapper = switch (requestParam.getQueryType()){
            case 0 -> Wrappers.lambdaQuery(RegionDO.class)
                    .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());
            case 1 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.A_E.getSpells());
            case 2 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.F_J.getSpells());
            case 3 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.K_O.getSpells());
            case 4 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.P_T.getSpells());
            case 5 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.U_Z.getSpells());
            default -> throw new ClientException("查询失败，请检查查询参数是否正确");
        };
        return safeGetRegionStation(
                key,
                () ->{
                    List<RegionDO> regionDOS = regionMapper.selectList(queryWrapper);
                    return JSON.toJSONString(BeanUtil.convert(regionDOS, RegionStationQueryRespDTO.class));
                },
                String.valueOf(requestParam.getQueryType())
        );
    }

    @Override
    public List<StationQueryRespDTO> listAllStation() {
        return distributedCache.safeGet(
                RedisKeyConstant.STATION_ALL,
                List.class,
                () -> BeanUtil.convert(
                        stationMapper.selectList(Wrappers.emptyWrapper()) ,
                        StationQueryRespDTO.class
                ),
                As12306Constant.ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
    }

    private List<RegionStationQueryRespDTO> safeGetRegionStation(final String key,
                                                                 CacheLoader<String> loader,
                                                                 String param){
        List<RegionStationQueryRespDTO> result;
        if(CollUtil.isNotEmpty(result = JSON.parseArray(
                distributedCache.get(key,String.class),
                RegionStationQueryRespDTO.class))
        ){
            return result;
        }
        String lockKey = String.format(RedisKeyConstant.LOCK_QUERY_REGION_STATION_LIST, param);
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            if(CollUtil.isEmpty(result = JSON.parseArray(distributedCache.get(key, String.class), RegionStationQueryRespDTO.class))){
                if(CollUtil.isEmpty(result = loadAndSet(key,loader))){
                    return Collections.emptyList();
                }
            }
        }finally {
            lock.unlock();
        }
        return result;
    }

    private List<RegionStationQueryRespDTO> loadAndSet(final String key,CacheLoader<String> loader){
        String result = loader.load();
        if(CacheUtil.isNullOrBlank(result)){
            return Collections.emptyList();
        }
        List<RegionStationQueryRespDTO> respDTOS = JSON.parseArray(result, RegionStationQueryRespDTO.class);
        distributedCache.put(
                key,
                result,
                As12306Constant.ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
        return respDTOS;
    }
}
