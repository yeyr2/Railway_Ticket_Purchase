package org.yey2.as12306.ticketService.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.yey2.as12306.ticketService.common.constant.RedisKeyConstant;
import org.yey2.as12306.ticketService.dao.entity.CarriageDO;
import org.yey2.as12306.ticketService.dao.mapper.CarriageMapper;
import org.yey2.as12306.ticketService.service.CarriageService;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.cache.core.CacheLoader;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 列车车厢接口层实现
 */
@Service
@AllArgsConstructor
public class CarriageServiceImpl implements CarriageService {

    private DistributedCache distributedCache;
    private CarriageMapper carriageMapper;
    private RedissonClient redissonClient;

    @Override
    public List<String> listCarriageNumber(String trainId, Integer carriageType) {
        final String key = RedisKeyConstant.TRAIN_CARRIAGE + trainId;
        return safeGetCarriageNumber(
                trainId,
                key,
                carriageType,
                () -> {
                    LambdaQueryWrapper<CarriageDO> queryWrapper = Wrappers.lambdaQuery(CarriageDO.class)
                            .eq(CarriageDO::getTrainId,trainId)
                            .eq(CarriageDO::getCarriageType,carriageType);
                    List<CarriageDO> carriageDOS = carriageMapper.selectList(queryWrapper);
                    List<String> carriageList = carriageDOS.stream().map(CarriageDO::getCarriageNumber).collect(Collectors.toList());
                    return StrUtil.join(StrUtil.COMMA, carriageList);
                }
        );
    }


    /**
     * 获取Redis的Hash数据结构进行操作的工具类
     */
    private HashOperations<String, Object, Object> getHashOperations() {
        StringRedisTemplate stringRedisTemplate =(StringRedisTemplate) distributedCache.getInstance();
        return stringRedisTemplate.opsForHash();
    }

    private String getCarriageNumber(final String key,Integer carriageType) {
        HashOperations<String,Object,Object> hashOperations = getHashOperations();
        return Optional.ofNullable(hashOperations.get(key,String.valueOf(carriageType))).map(Object::toString).orElse("");
    }

    private List<String> safeGetCarriageNumber(String trainId, final String key, Integer carriageType, CacheLoader<String> loader){
        String result = getCarriageNumber(key,carriageType);
        if(!CacheUtil.isNullOrBlank(result)){
            return StrUtil.split(result,StrUtil.COMMA);
        }
        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_QUERY_CARRIAGE_NUMBER_LIST, trainId));
        lock.lock();
        try{
            if(CacheUtil.isNullOrBlank(result = getCarriageNumber(key,carriageType))){
                if(CacheUtil.isNullOrBlank(result = loadAndSet(key,carriageType,loader))){
                    return Collections.emptyList();
                }
            }
        }finally {
            lock.unlock();
        }
        return StrUtil.split(result,StrUtil.COMMA);
    }

    private String loadAndSet(final String key,Integer carriageType,CacheLoader<String> loader){
        String result = loader.load();
        if(CacheUtil.isNullOrBlank(result)){
            return result;
        }
        HashOperations<String,Object,Object> hashOperations = getHashOperations();
        hashOperations.putIfAbsent(key,String.valueOf(carriageType),result);
        return result;
    }
}
