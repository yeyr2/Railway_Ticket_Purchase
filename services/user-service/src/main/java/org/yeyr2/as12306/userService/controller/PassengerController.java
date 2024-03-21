package org.yeyr2.as12306.userService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yeyr2.as12306.bizs.user.core.UserContext;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;
import org.yeyr2.as12306.idempotent.enums.IdempotentSceneEnum;
import org.yeyr2.as12306.idempotent.enums.IdempotentTypeEnum;
import org.yeyr2.as12306.userService.dao.entity.PassengerDO;
import org.yeyr2.as12306.userService.dto.req.PassengerRemoveReqDTO;
import org.yeyr2.as12306.userService.dto.req.PassengerReqDTO;
import org.yeyr2.as12306.userService.dto.resp.PassengerActualRespDTO;
import org.yeyr2.as12306.userService.dto.resp.PassengerRespDTO;
import org.yeyr2.as12306.userService.service.PassengerService;
import org.yeyr2.as12306.web.Results;

import java.util.List;

/**
 * 乘车人控制层
 */
@RestController
@RequiredArgsConstructor
public class PassengerController {

    private PassengerService passengerService;

    /**
     * 根据用户名查询乘车人列表
     */
    @GetMapping("/api/user-service/passenger/query")
    public Result<List<PassengerRespDTO>> listPassengerQueryByUsername(){
        return Results.success(passengerService.listPassengerQueryByUsername(UserContext.getUsername()));
    }

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/inner/passenger/actual/query/ids")
    public Result<List<PassengerActualRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username,@RequestParam("ids") List<Long> ids ){
        return Results.success(passengerService.listPassengerQueryByIds(username, ids));
    }

    /**
     * 新增乘车人
     */
    @Idempotent(
            uniqueKeyPrefix = "as12306-user:lock_password-alter:",
            key = "T(org.yeyr2.as12306.bizs.user.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI,
            message = "正在修改乘车人，请稍后再试..."
    )
    @PostMapping("/api/user-service/passenger/save")
    public Result<Void> savePassenger(@RequestParam PassengerReqDTO reqDTO){
        passengerService.savePassenger(reqDTO);
        return Results.success();
    }


    /**
     * 修改乘车人
     */
    @Idempotent(
            uniqueKeyPrefix = "as12306-user:lock_passenger-alter:",
            key = "T(org.yeyr2.as12306.bizs.user.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI,
            message = "正在修改乘车人，请稍后再试.."
    )
    @PostMapping("/api/user-service/passenger/update")
    public Result<Void> updatePassenger(@RequestParam PassengerReqDTO reqDTO){
        passengerService.updatePassenger(reqDTO);
        return Results.success();
    }

    /**
     * 移除乘车人
     */
    @Idempotent(
            uniqueKeyPrefix = "index12306-user:lock_passenger-alter:",
            key = "T(org.opengoofy.index12306.frameworks.starter.user.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI,
            message = "正在移除乘车人，请稍后再试..."
    )
    @PostMapping("/api/user-service/passenger/remove")
    public Result<Void> removePassenger(@RequestBody PassengerRemoveReqDTO requestParam) {
        passengerService.removePassenger(requestParam);
        return Results.success();
    }
}
