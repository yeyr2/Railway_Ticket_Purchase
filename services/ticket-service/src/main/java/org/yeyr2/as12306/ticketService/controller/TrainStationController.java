package org.yeyr2.as12306.ticketService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeyr2.as12306.ticketService.dto.resp.TrainStationQueryRespDTO;
import org.yeyr2.as12306.ticketService.service.TrainStationService;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.web.Results;

import java.util.List;

/**
 * 列车站点控制层
 */
@RestController
@RequiredArgsConstructor
public class TrainStationController {

    private final TrainStationService trainStationService;

    /**
     * 根据列车 ID 查询站点信息
     */
    @GetMapping("/api/ticket-service/train-station/query")
    public Result<List<TrainStationQueryRespDTO>> listTrainStationQuery(@RequestParam("trainId") String trainId){
        return Results.success(trainStationService.listTrainStationQuery(trainId));
    }
}
