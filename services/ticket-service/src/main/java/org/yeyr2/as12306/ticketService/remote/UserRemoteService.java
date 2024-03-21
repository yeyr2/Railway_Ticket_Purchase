package org.yeyr2.as12306.ticketService.remote;

import org.apache.ibatis.annotations.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.yeyr2.as12306.ticketService.remote.dto.resp.PassengerRespDTO;
import org.yeyr2.as12306.convention.result.Result;

import java.util.List;

/**
 * 用户远程服务调用
 */
@FeignClient(value = "as112306-user${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface UserRemoteService {

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/inner/passenger/actual/query/ids")
    Result<List<PassengerRespDTO>> listPassengerQueryByIds(@RequestParam("username") @Param("username") String username, @RequestParam("ids") @Param("ids") List<String> ids);

}
