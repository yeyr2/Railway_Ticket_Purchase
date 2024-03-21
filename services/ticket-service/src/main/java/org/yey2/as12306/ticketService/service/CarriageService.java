package org.yey2.as12306.ticketService.service;

import java.util.List;

/**
 * 列车车厢接口层
 */
public interface CarriageService {

    /**
     * 查询列车车厢号集合
     * @param trainId      列车 ID
     * @param carriageType 车厢类型
     * @return 车厢号集合
     */
    List<String> listCarriageNumber(String trainId, Integer carriageType);
}
