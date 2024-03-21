package org.yey2.as12306.ticketService.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yey2.as12306.ticketService.dao.entity.TrainStationDO;
import org.yey2.as12306.ticketService.dao.mapper.TrainStationMapper;
import org.yey2.as12306.ticketService.dto.domain.RouteDTO;
import org.yey2.as12306.ticketService.dto.resp.TrainStationQueryRespDTO;
import org.yey2.as12306.ticketService.service.TrainStationService;
import org.yey2.as12306.ticketService.toolkit.StationCalculateUtil;
import org.yeyr2.as12306.common.toolkit.BeanUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 列车站点接口实现层
 */
@Service
@RequiredArgsConstructor
public class TrainStationServiceImpl implements TrainStationService {

    private final TrainStationMapper trainStationMapper;


    @Override
    public List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId,trainId);
        List<TrainStationDO> trainStationDOS = trainStationMapper.selectList(queryWrapper);
        return BeanUtil.convert(trainStationDOS, TrainStationQueryRespDTO.class);
    }

    @Override
    public List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId,trainId)
                .select(TrainStationDO::getDeparture);
        List<TrainStationDO> trainStationDOS = trainStationMapper.selectList(queryWrapper);
        List<String> trainStationAllList = trainStationDOS.stream().map(TrainStationDO::getDeparture).collect(Collectors.toList());
        return StationCalculateUtil.throughStation(trainStationAllList,departure,arrival);
    }

    @Override
    public List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId,trainId)
                .select(TrainStationDO::getDeparture);
        List<TrainStationDO> trainStationDOS = trainStationMapper.selectList(queryWrapper);
        List<String> trainStationAllList = trainStationDOS.stream().map(TrainStationDO::getDeparture).collect(Collectors.toList());
        return StationCalculateUtil.takeoutStation(trainStationAllList,departure,arrival);
    }
}
