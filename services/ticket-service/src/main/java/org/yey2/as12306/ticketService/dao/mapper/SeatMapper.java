package org.yey2.as12306.ticketService.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.yey2.as12306.ticketService.dao.entity.SeatDO;
import org.yey2.as12306.ticketService.dto.domain.SeatTypeCountDTO;

import java.util.List;

/**
 * 座位持久层
 */
public interface SeatMapper extends BaseMapper<SeatDO> {

    /**
     * 获取列车车厢余票集合
     */
    List<Integer> listSeatRemainingTicket(@Param("seatDO") SeatDO seatDO, @Param("trainCarriageList") List<String> trainCarriageList);

    /**
     * 获取列车 startStation 到 endStation 区间可用座位集合
     */
    List<SeatTypeCountDTO> listSeatTypeCount(@Param("trainId") Long trainId, @Param("startStation") String startStation, @Param("endStation") String endStation, @Param("seatTypes")  List<Integer> seatTypes);


}
