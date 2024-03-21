package org.yey2.as12306.ticketService.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yey2.as12306.ticketService.common.constant.As12306Constant;
import org.yey2.as12306.ticketService.common.enums.*;
import org.yey2.as12306.ticketService.dao.entity.*;
import org.yey2.as12306.ticketService.dao.mapper.*;
import org.yey2.as12306.ticketService.dto.domain.PurchaseTicketPassengerDetailDTO;
import org.yey2.as12306.ticketService.dto.domain.RouteDTO;
import org.yey2.as12306.ticketService.dto.domain.SeatClassDTO;
import org.yey2.as12306.ticketService.dto.domain.TicketListDTO;
import org.yey2.as12306.ticketService.dto.req.*;
import org.yey2.as12306.ticketService.dto.resp.RefundTicketRespDTO;
import org.yey2.as12306.ticketService.dto.resp.TicketOrderDetailRespDTO;
import org.yey2.as12306.ticketService.dto.resp.TicketPageQueryRespDTO;
import org.yey2.as12306.ticketService.dto.resp.TicketPurchaseRespDTO;
import org.yey2.as12306.ticketService.remote.PayRemoteService;
import org.yey2.as12306.ticketService.remote.TicketOrderRemoteService;
import org.yey2.as12306.ticketService.remote.dto.req.RefundReqDTO;
import org.yey2.as12306.ticketService.remote.dto.req.TicketOrderCreateRemoteReqDTO;
import org.yey2.as12306.ticketService.remote.dto.req.TicketOrderItemCreateRemoteReqDTO;
import org.yey2.as12306.ticketService.remote.dto.resp.PayInfoRespDTO;
import org.yey2.as12306.ticketService.remote.dto.resp.RefundRespDTO;
import org.yey2.as12306.ticketService.remote.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.yey2.as12306.ticketService.service.SeatService;
import org.yey2.as12306.ticketService.service.TicketService;
import org.yey2.as12306.ticketService.service.TrainStationService;
import org.yey2.as12306.ticketService.service.cache.SeatMarginCacheLoader;
import org.yey2.as12306.ticketService.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.yey2.as12306.ticketService.service.handler.ticket.select.TrainSeatTypeSelector;
import org.yey2.as12306.ticketService.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import org.yey2.as12306.ticketService.toolkit.DateUtil;
import org.yey2.as12306.ticketService.toolkit.TimeStationComparator;
import org.yeyr2.as12306.base.ApplicationContextHolder;
import org.yeyr2.as12306.bizs.user.core.UserContext;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.cache.toolkit.CacheUtil;
import org.yeyr2.as12306.common.toolkit.BeanUtil;
import org.yeyr2.as12306.convention.exception.ServiceException;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.designpattern.chain.AbstractChainContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.yey2.as12306.ticketService.common.constant.RedisKeyConstant.*;

/**
 * 车票接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TickerServiceImpl extends ServiceImpl<TicketMapper, TicketDO> implements TicketService, CommandLineRunner {

    private final TrainMapper trainMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final DistributedCache distributedCache;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final PayRemoteService payRemoteService;
    private final StationMapper stationMapper;
    private final SeatService seatService;
    private final TrainStationService trainStationService;
    private final TrainSeatTypeSelector trainSeatTypeSelector;
    private final SeatMarginCacheLoader seatMarginCacheLoader;
    private final AbstractChainContext<TicketPageQueryReqDTO> ticketPageQueryReqDTOAbstractChainContext;
    private final AbstractChainContext<PurchaseTicketReqDTO> purchaseTicketReqDTOAbstractChainContext;
    private final AbstractChainContext<RefundTicketReqDTO> refundTicketReqDTOAbstractChainContext;
    private final RedissonClient redissonClient;
    private final ConfigurableEnvironment configurableEnvironment;
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;
    // 为了保证事务的正常运行
    private TicketService ticketService;

    @Value("${ticket.availability.cache-update.type:}")
    private String ticketAvailabilityCacheUpdateType;
    @Value("${framework.cache.redis.prefix:}")
    private String cacheRedisPrefix;

    private final Cache<String, ReentrantLock> localLockMap = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    @Override
    public void run(String... args) throws Exception {
        ticketService = ApplicationContextHolder.getBean(TicketService.class);
    }

    /**
     * v1 版本,版本存在严重的性能深渊问题,性能有待提高
     */
    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV1(TicketPageQueryReqDTO requestParam) {
        // 责任链模式 验证城市名称是否存在、不存在加载缓存以及出发日期不能小于当前日期等等
        ticketPageQueryReqDTOAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(),requestParam);
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        List<Object> stationDetails = stringRedisTemplate.opsForHash()
                .multiGet(REGION_TRAIN_STATION_MAPPING,
                        Lists.newArrayList(requestParam.getFromStation(),requestParam.getToStation()));
        long count = stationDetails.stream().filter(Objects::isNull).count();
        if(count > 0){
            // 如果查不到,就上锁再次查询,若还是没有就将站点信息,再次去数据库查询并写入redis
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION_MAPPING);
            lock.lock();
            try{
                stationDetails = stringRedisTemplate.opsForHash()
                        .multiGet(REGION_TRAIN_STATION_MAPPING,
                                Lists.newArrayList(requestParam.getFromStation(),requestParam.getToStation()));
                count = stationDetails.stream().filter(Objects::isNull).count();
                if(count > 0){
                    List<StationDO> stationDOS = stationMapper.selectList(Wrappers.emptyWrapper());
                    Map<String,String> regionTrainStationMap = new HashMap<>();
                    stationDOS.forEach(each -> regionTrainStationMap.put(each.getCode(),each.getRegionName()));
                    stringRedisTemplate.opsForHash()
                            .putAll(REGION_TRAIN_STATION_MAPPING,regionTrainStationMap);
                    stationDetails = new ArrayList<>();
                    stationDetails.add(regionTrainStationMap.get(requestParam.getFromStation()));
                    stationDetails.add(regionTrainStationMap.get(requestParam.getToStation()));
                }
            }finally {
                lock.unlock();
            }
        }
        // 获取站点信息
        List<TicketListDTO> seatResults = new ArrayList<>();
        String buildRegionTrainStationHashKey = String.format(REGION_TRAIN_STATION,stationDetails.get(0),stationDetails.get(1));
        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
        if(MapUtil.isEmpty(regionTrainStationAllMap)){
            // 获取车次信息并存入redis
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION);
            lock.lock();
            try{
                regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
                if(MapUtil.isEmpty(regionTrainStationAllMap)){
                    LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getStartRegion,stationDetails.get(0))
                            .eq(TrainStationRelationDO::getEndRegion,stationDetails.get(1));
                    List<TrainStationRelationDO> trainStationRelationDOS = trainStationRelationMapper.selectList(queryWrapper);
                    // 6
                    for (TrainStationRelationDO each : trainStationRelationDOS) {
                        TrainDO trainDO = distributedCache.safeGet(
                                TRAIN_INFO + each.getTrainId(),
                                TrainDO.class,
                                () -> trainMapper.selectById(each.getTrainId()),
                                As12306Constant.ADVANCE_TICKET_DAY,
                                TimeUnit.DAYS
                        );
                        TicketListDTO result = new TicketListDTO();
                        result.setTrainId(String.valueOf(trainDO.getId()));
                        result.setTrainNumber(trainDO.getTrainNumber());
                        result.setDepartureTime(DateUtil.convertDateToLocalTime(each.getDepartureTime(),"HH:mm"));
                        result.setArrivalTime(DateUtil.convertDateToLocalTime(each.getArrivalTime(),"HH:mm"));
                        result.setDuration(DateUtil.calculateHourDifference(each.getDepartureTime(),each.getArrivalTime()));
                        result.setDeparture(each.getDeparture());
                        result.setArrival(each.getArrival());
                        result.setDepartureFlag(each.getDepartureFlag());
                        result.setArrivalFlag(each.getArrivalFlag());
                        result.setTrainType(trainDO.getTrainType());
                        result.setTrainBrand(trainDO.getTrainBrand());
                        if (StrUtil.isNotBlank(trainDO.getTrainTag())) {
                            result.setTrainTags(StrUtil.split(trainDO.getTrainTag(), ","));
                        }
                        long betweenDay = cn.hutool.core.date.DateUtil.betweenDay(each.getDepartureTime(),each.getArrivalTime(),false);
                        result.setDaysArrived((int) betweenDay);
                        result.setSaleStatus(new Date().after(trainDO.getSaleTime()) ? 0 : 1);
                        result.setSaleTime(DateUtil.convertDateToLocalTime(trainDO.getSaleTime(),"MM-dd HH:mm"));
                        seatResults.add(result);
                        regionTrainStationAllMap.put(CacheUtil.buildKey(String.valueOf(each.getTrainId()),each.getDeparture(),each.getArrival()), JSON.toJSONString(result));
                    }
                    stringRedisTemplate.opsForHash().putAll(buildRegionTrainStationHashKey,regionTrainStationAllMap);
                }
            }finally {
                lock.unlock();
            }
        }
        seatResults = CollUtil.isEmpty(seatResults)
                ? regionTrainStationAllMap.values().stream().map(each -> JSON.parseObject(each.toString(), TicketListDTO.class)).collect(Collectors.toList())
                : seatResults;
        seatResults = seatResults.stream().sorted(new TimeStationComparator()).collect(Collectors.toList());
        // 获取各个车次的座位价格
        for (TicketListDTO each : seatResults) {
            String trainStationPriceStr = distributedCache.safeGet(
                    String.format(TRAIN_STATION_PRICE,each.getTrainId(),each.getDeparture(),each.getArrival()),
                    String.class,
                    () -> {
                        LambdaQueryWrapper<TrainStationPriceDO> queryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                                .eq(TrainStationPriceDO::getDeparture,each.getDeparture())
                                .eq(TrainStationPriceDO::getArrival,each.getArrival())
                                .eq(TrainStationPriceDO::getTrainId,each.getTrainId());
                        return JSON.toJSONString(trainStationPriceMapper.selectList(queryWrapper));
                    },
                    As12306Constant.ADVANCE_TICKET_DAY,
                    TimeUnit.DAYS
            );
            List<TrainStationPriceDO> trainStationPriceDOS = JSON.parseArray(trainStationPriceStr, TrainStationPriceDO.class);
            List<SeatClassDTO> seatClassDTOS = new ArrayList<>();
            // 获取对应种类的座位的价格
            trainStationPriceDOS.forEach(item -> {
                String seatType = String.valueOf(item.getSeatType());
                String keySuffix = CacheUtil.buildKey(each.getTrainId(),item.getDeparture(),item.getArrival());
                Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix , seatType);
                int quantity = Optional.ofNullable(quantityObj)
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElseGet(() -> {
                            Map<String, String> seatMarginMap = seatMarginCacheLoader.load(String.valueOf(each.getTrainId()), seatType, item.getDeparture(), item.getArrival());
                            return Optional.ofNullable(seatMarginMap.get(String.valueOf(item.getSeatType()))).map(Integer::parseInt).orElse(0);
                        });
                seatClassDTOS.add(new SeatClassDTO(item.getSeatType(), quantity, new BigDecimal(item.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP), false));
            });
            each.setSeatClassList(seatClassDTOS);
        }
        return TicketPageQueryRespDTO.builder()
                .trainList(seatResults)
                .departureStationList(buildDepartureStationList(seatResults))
                .arrivalStationList(buildArrivalStationList(seatResults))
                .trainBrandList(buildTrainBrandList(seatResults))
                .seatClassTypeList(buildSeatClassList(seatResults))
                .build();
    }

    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV2(TicketPageQueryReqDTO requestParam) {
        // 责任链模式 验证城市名称是否存在、不存在加载缓存以及出发日期不能小于当前日期等等
        ticketPageQueryReqDTOAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(), requestParam);
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        List<Object> stationDetails = stringRedisTemplate.opsForHash()
                .multiGet(REGION_TRAIN_STATION_MAPPING, Lists.newArrayList(requestParam.getFromStation() , requestParam.getToStation()));
        // todo: 如何保证REDIS里有信息
        // 获取车次信息
        String buildRegionTrainStationHashKey = String.format(REGION_TRAIN_STATION, stationDetails.get(0),stationDetails.get(1));
        Map<Object,Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
        List<TicketListDTO> seatResults = regionTrainStationAllMap.values().stream()
                .map(each -> JSON.parseObject(each.toString(), TicketListDTO.class))
                .sorted(new TimeStationComparator())
                .toList();
        // 获取票价
        List<String> trainStationPriceKeys = seatResults.stream()
                .map(each -> String.format(cacheRedisPrefix + TRAIN_STATION_PRICE,each.getTrainId(),each.getDeparture(),each.getArrival()))
                .toList();
        List<Object> trainStationPriceObjs = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {
            trainStationPriceKeys.forEach(each -> connection.stringCommands().get(each.getBytes()));
            return null;
        });
        List<TrainStationPriceDO> trainStationPriceDOS = new ArrayList<>();
        List<String> trainStationRemainingKeyList = new ArrayList<>();
        // 反序列化票信息并准备查询余票数量的key
        trainStationPriceObjs.forEach(each -> {
                    List<TrainStationPriceDO> trainStationPriceList = JSON.parseArray(each.toString(), TrainStationPriceDO.class);
                    trainStationPriceDOS.addAll(trainStationPriceList);
                    trainStationPriceList.forEach(item -> {
                        String trainStationRemainingKey = cacheRedisPrefix + TRAIN_STATION_REMAINING_TICKET + CacheUtil.buildKey(String.valueOf(item.getTrainId()), item.getDeparture(), item.getArrival());
                        trainStationRemainingKeyList.add(trainStationRemainingKey);
                    });
                }
        );
        /// 查询余票数量
        List<Object> trainStationRemainingObjs = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {
            for(int i = 0 ; i < trainStationRemainingKeyList.size() ; i++){
                connection.hashCommands().hGet(trainStationRemainingKeyList.get(i).getBytes(), trainStationPriceDOS.get(i).getSeatType().toString().getBytes());
            }
            return null;
        });

        // 获取座位具体信息
        for (TicketListDTO each : seatResults) {
            // todo: 为什么不直接用获取的信息
            List<Integer> seatTypesByCode = VehicleTypeEnum.findSeatTypesByCode(each.getTrainType());
            List<Object> remainingTicket = new ArrayList<>(trainStationRemainingObjs.subList(0, seatTypesByCode.size()));
            List<TrainStationPriceDO> trainStationPriceDOList = new ArrayList<>(trainStationPriceDOS.subList(0, seatTypesByCode.size()));
            trainStationRemainingObjs.subList(0, seatTypesByCode.size()).clear();
            trainStationPriceDOS.subList(0, seatTypesByCode.size()).clear();
            List<SeatClassDTO> seatClassDTOS = new ArrayList<>();
            for (int i = 0; i < trainStationPriceDOList.size(); i++) {
                TrainStationPriceDO trainStationPriceDO = trainStationPriceDOList.get(i);
                SeatClassDTO seatClassDTO = SeatClassDTO.builder()
                        .type(trainStationPriceDO.getSeatType())
                        .quantity(Integer.parseInt(remainingTicket.get(i).toString()))
                        .price(new BigDecimal(trainStationPriceDO.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP))
                        .candidate(false)
                        .build();
                seatClassDTOS.add(seatClassDTO);
            }
            each.setSeatClassList(seatClassDTOS);
        }

        return TicketPageQueryRespDTO.builder()
                .trainList(seatResults)
                .departureStationList(buildDepartureStationList(seatResults))
                .arrivalStationList(buildArrivalStationList(seatResults))
                .trainBrandList(buildTrainBrandList(seatResults))
                .seatClassTypeList(buildSeatClassList(seatResults))
                .build();
    }

    @Override
    public TicketPurchaseRespDTO purchaseTicketsV1(PurchaseTicketReqDTO requestParam) {
        // 责任链模式，验证 1：参数必填 2：参数正确性 3：乘客是否已买当前车次等...
        purchaseTicketReqDTOAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(),requestParam);
        String lockKey = configurableEnvironment.resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS,requestParam.getTrainId()));
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try{
            return ticketService.executePurchaseTickets(requestParam);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public TicketPurchaseRespDTO purchaseTicketsV2(PurchaseTicketReqDTO requestParam) {
        // 责任链模式，验证 1：参数必填 2：参数正确性 3：乘客是否已买当前车次等...
        purchaseTicketReqDTOAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(), requestParam);
        boolean tokenResult = ticketAvailabilityTokenBucket.takeTokenFromBucket(requestParam);
        if(!tokenResult){
            throw new ServiceException("列车站点已无余票");
        }
        List<ReentrantLock> localLockList = new ArrayList<>();
        List<RLock> distributedLockList = new ArrayList<>();
        // 查询redis用于
        Map<Integer,List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        seatTypeMap.forEach((seatType, count) -> {
            // 解析占位符
            String lockKey = configurableEnvironment.resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS_V2,requestParam.getTrainId(),seatType));
            // 获取锁
            ReentrantLock localLock = localLockMap.getIfPresent(lockKey);
            if(localLock == null){
                synchronized (TicketService.class){
                    if((localLock = localLockMap.getIfPresent(lockKey)) == null){
                        localLock = new ReentrantLock(true);
                        localLockMap.put(lockKey,localLock);
                    }
                }
            }
            localLockList.add(localLock);
            RLock lock = redissonClient.getFairLock(lockKey);
            distributedLockList.add(lock);
        });
        try{
            localLockList.forEach(ReentrantLock::lock);
            distributedLockList.forEach(RLock::lock);
            return ticketService.executePurchaseTickets(requestParam);
        }finally {
            localLockList.forEach(ReentrantLock::unlock);
            distributedLockList.forEach(RLock::unlock);
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public TicketPurchaseRespDTO executePurchaseTickets(PurchaseTicketReqDTO requestParam) {
        List<TicketOrderDetailRespDTO> ticketOrderDetailResults = new ArrayList<>();
        String trainId = requestParam.getTrainId();
        // 获取列车信息
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + trainId,
                TrainDO.class,
                () -> trainMapper.selectById(trainId),
                As12306Constant.ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );

        // 重点,获取完整的乘车人购票信息
        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketRespDTOS = trainSeatTypeSelector.select(trainDO.getTrainType(),requestParam);
        // 将车票存入数据库
        List<TicketDO> ticketDOS = trainPurchaseTicketRespDTOS.stream()
                .map(each -> TicketDO.builder()
                        .username(UserContext.getUsername())
                        .trainId(Long.parseLong(requestParam.getTrainId()))
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .passengerId(each.getPassengerId())
                        .ticketStatus(TicketStatusEnum.UNPAID.getCode())
                        .build()).toList();
        saveBatch(ticketDOS);

        Result<String> ticketOrederResult;
        try{
            List<TicketOrderItemCreateRemoteReqDTO> orderItemCreateRemoteReqDTOS = new ArrayList<>();
            trainPurchaseTicketRespDTOS.forEach(each -> {
                // 创建订单明细(车票信息),作为订单请求的详细信息,用于发送远程调用订单
                TicketOrderItemCreateRemoteReqDTO orderItemCreateRemoteReqDTO = TicketOrderItemCreateRemoteReqDTO.builder()
                        .amount(each.getAmount())
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .idCard(each.getIdCard())
                        .idType(each.getIdType())
                        .phone(each.getPhone())
                        .seatType(each.getSeatType())
                        .ticketType(each.getUserType())
                        .realName(each.getRealName())
                        .build();
                // 创建订单信息,用于返回给用户
                TicketOrderDetailRespDTO ticketOrderDetailRespDTO = TicketOrderDetailRespDTO.builder()
                        .amount(each.getAmount())
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .idCard(each.getIdCard())
                        .idType(each.getIdType())
                        .seatType(each.getSeatType())
                        .ticketType(each.getUserType())
                        .realName(each.getRealName())
                        .build();
                orderItemCreateRemoteReqDTOS.add(orderItemCreateRemoteReqDTO);
                ticketOrderDetailResults.add(ticketOrderDetailRespDTO);
            });
            // 查询数据库获取该车次的站点信息(例如出发时间和抵达时间)
            LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, trainId)
                    .eq(TrainStationRelationDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationRelationDO::getArrival, requestParam.getArrival());
            TrainStationRelationDO trainStationRelationDO = trainStationRelationMapper.selectOne(queryWrapper);
            // 创建订单请求信息,用于远程调用订单服务
            TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO = TicketOrderCreateRemoteReqDTO.builder()
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderTime(new Date())
                    .source(SourceEnum.INTERNET.getCode())
                    .trainNumber(trainDO.getTrainNumber())
                    .departureTime(trainStationRelationDO.getDepartureTime())
                    .arrivalTime(trainStationRelationDO.getArrivalTime())
                    .ridingDate(trainStationRelationDO.getDepartureTime())
                    .userId(UserContext.getUserId())
                    .username(UserContext.getUsername())
                    .trainId(Long.parseLong(requestParam.getTrainId()))
                    .ticketOrderItems(orderItemCreateRemoteReqDTOS)
                    .build();
            ticketOrederResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
            if (!ticketOrederResult.isSuccess() || StrUtil.isBlank(ticketOrederResult.getData())) {
                log.error("订单服务调用失败，返回结果：{}", ticketOrederResult.getMessage());
                throw new ServiceException("订单服务调用失败");
            }
        } catch (Throwable ex) {
            log.error("远程调用订单服务创建错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
            throw ex;
        }
        return new TicketPurchaseRespDTO(ticketOrederResult.getData(), ticketOrderDetailResults);
    }

    @Override
    public PayInfoRespDTO getPayInfo(String orderSn) {
        return payRemoteService.getPayInfo(orderSn).getData();
    }

    @Override
    public void cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {
        Result<Void> cancelOrderResult = ticketOrderRemoteService.cancelTicketOrder(requestParam);
        if (cancelOrderResult.isSuccess() && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            Result<org.yey2.as12306.ticketService.remote.dto.resp.TicketOrderDetailRespDTO> ticketOrderDetailResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(requestParam.getOrderSn());
            org.yey2.as12306.ticketService.remote.dto.resp.TicketOrderDetailRespDTO ticketOrderDetail = ticketOrderDetailResult.getData();
            String trainId = String.valueOf(ticketOrderDetail.getTrainId());
            String departure = ticketOrderDetail.getDeparture();
            String arrival = ticketOrderDetail.getArrival();
            List<TicketOrderPassengerDetailRespDTO> trainPurchaseTicketResults = ticketOrderDetail.getPassengerDetails();
            try {
                seatService.unlock(trainId, departure, arrival, BeanUtil.convert(trainPurchaseTicketResults, TrainPurchaseTicketRespDTO.class));
            } catch (Throwable ex) {
                log.error("[取消订单] 订单号：{} 回滚列车DB座位状态失败", requestParam.getOrderSn(), ex);
                throw ex;
            }
            ticketAvailabilityTokenBucket.rollbackInBucket(ticketOrderDetail);
            try {
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                Map<Integer, List<TicketOrderPassengerDetailRespDTO>> seatTypeMap = trainPurchaseTicketResults.stream()
                        .collect(Collectors.groupingBy(TicketOrderPassengerDetailRespDTO::getSeatType));
                List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);
                routeDTOList.forEach(each -> {
                    String keySuffix = StrUtil.join("_", trainId, each.getStartStation(), each.getEndStation());
                    seatTypeMap.forEach((seatType, ticketOrderPassengerDetailRespDTOList) -> {
                        stringRedisTemplate.opsForHash()
                                .increment(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType), ticketOrderPassengerDetailRespDTOList.size());
                    });
                });
            } catch (Throwable ex) {
                log.error("[取消关闭订单] 订单号：{} 回滚列车Cache余票失败", requestParam.getOrderSn(), ex);
                throw ex;
            }
        }
    }

    @Override
    public RefundTicketRespDTO commonTicketRefund(RefundTicketReqDTO requestParam) {
        // 责任链模式，验证 1：参数必填
        refundTicketReqDTOAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_REFUND_TICKET_FILTER.name(), requestParam);
        Result<org.yey2.as12306.ticketService.remote.dto.resp.TicketOrderDetailRespDTO> orderDetailRespDTOResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(requestParam.getOrderSn());
        if (!orderDetailRespDTOResult.isSuccess() && Objects.isNull(orderDetailRespDTOResult.getData())) {
            throw new ServiceException("车票订单不存在");
        }
        org.yey2.as12306.ticketService.remote.dto.resp.TicketOrderDetailRespDTO ticketOrderDetailRespDTO = orderDetailRespDTOResult.getData();
        List<TicketOrderPassengerDetailRespDTO> passengerDetails = ticketOrderDetailRespDTO.getPassengerDetails();
        if (CollectionUtil.isEmpty(passengerDetails)) {
            throw new ServiceException("车票子订单不存在");
        }
        RefundReqDTO refundReqDTO = new RefundReqDTO();
        if (RefundTypeEnum.PARTIAL_REFUND.getType().equals(requestParam.getType())) {
            TicketOrderItemQueryReqDTO ticketOrderItemQueryReqDTO = new TicketOrderItemQueryReqDTO();
            ticketOrderItemQueryReqDTO.setOrderSn(requestParam.getOrderSn());
            ticketOrderItemQueryReqDTO.setOrderItemRecordIds(requestParam.getSubOrderRecordIdReqList());
            Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById = ticketOrderRemoteService.queryTicketItemOrderById(ticketOrderItemQueryReqDTO);
            List<TicketOrderPassengerDetailRespDTO> partialRefundPassengerDetails = passengerDetails.stream()
                    .filter(item -> queryTicketItemOrderById.getData().contains(item))
                    .collect(Collectors.toList());
            refundReqDTO.setRefundTypeEnum(RefundTypeEnum.PARTIAL_REFUND);
            refundReqDTO.setRefundDetailReqDTOList(partialRefundPassengerDetails);
        } else if (RefundTypeEnum.FULL_REFUND.getType().equals(requestParam.getType())) {
            refundReqDTO.setRefundTypeEnum(RefundTypeEnum.FULL_REFUND);
            refundReqDTO.setRefundDetailReqDTOList(passengerDetails);
        }
        if (CollectionUtil.isNotEmpty(passengerDetails)) {
            Integer partialRefundAmount = passengerDetails.stream()
                    .mapToInt(TicketOrderPassengerDetailRespDTO::getAmount)
                    .sum();
            refundReqDTO.setRefundAmount(partialRefundAmount);
        }
        refundReqDTO.setOrderSn(requestParam.getOrderSn());
        Result<RefundRespDTO> refundRespDTOResult = payRemoteService.commonRefund(refundReqDTO);
        if (!refundRespDTOResult.isSuccess() && Objects.isNull(refundRespDTOResult.getData())) {
            throw new ServiceException("车票订单退款失败");
        }
        return null; // 暂时返回空实体
    }

    /**
     * 获取出发站点列表
     */
    private List<String> buildDepartureStationList(List<TicketListDTO> seatResults){
        return seatResults.stream().map(TicketListDTO::getDeparture).distinct().toList();
    }

    /**
     * 获取抵达站点列表
     */
    private List<String> buildArrivalStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getArrival).distinct().collect(Collectors.toList());
    }

    /**
     * 获取车次列表中所有座位类型
     */
    private List<Integer> buildSeatClassList(List<TicketListDTO> seatResults) {
        Set<Integer> resultSeatClassList = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            for (SeatClassDTO item : each.getSeatClassList()) {
                resultSeatClassList.add(item.getType());
            }
        }
        return resultSeatClassList.stream().toList();
    }

    /**
     * 获取车次列表中所有座列车品牌类型
     */
    private List<Integer> buildTrainBrandList(List<TicketListDTO> seatResults) {
        Set<Integer> trainBrandSet = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            if (StrUtil.isNotBlank(each.getTrainBrand())) {
                trainBrandSet.addAll(StrUtil.split(each.getTrainBrand(), ",").stream().map(Integer::parseInt).toList());
            }
        }
        return trainBrandSet.stream().toList();
    }

}
