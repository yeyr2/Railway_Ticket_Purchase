package org.yey2.as12306.ticketService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yey2.as12306.ticketService.dto.req.RegionStationQueryReqDTO;
import org.yey2.as12306.ticketService.dto.resp.RegionStationQueryRespDTO;
import org.yey2.as12306.ticketService.dto.resp.StationQueryRespDTO;
import org.yey2.as12306.ticketService.service.RegionStationService;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.web.Results;

import java.util.List;

/**
 * 地区以及车站查询控制层
 */
@RestController
@RequiredArgsConstructor
public class RegionStationController {

    private final RegionStationService regionStationService;

    /**
     * 查询车站及城市站点集合信息
     */
    @GetMapping("/api/ticket-service/region-station/query")
    public Result<List<RegionStationQueryRespDTO>> listRegionStation(RegionStationQueryReqDTO requestParam) {
        return Results.success(regionStationService.listRegionStation(requestParam));
    }

    /**
     * 查询车站站点集合信息
     */
    @GetMapping("/api/ticket-service/station/all")
    public Result<List<StationQueryRespDTO>> listAllStation() {
        return Results.success(regionStationService.listAllStation());
    }
}
