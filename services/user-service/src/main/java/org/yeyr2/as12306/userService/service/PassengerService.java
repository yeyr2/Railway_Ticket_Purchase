package org.yeyr2.as12306.userService.service;

import org.yeyr2.as12306.userService.dto.req.PassengerRemoveReqDTO;
import org.yeyr2.as12306.userService.dto.req.PassengerReqDTO;
import org.yeyr2.as12306.userService.dto.resp.PassengerActualRespDTO;
import org.yeyr2.as12306.userService.dto.resp.PassengerRespDTO;

import java.util.List;

/**
 * 乘车人接口层
 */
public interface PassengerService {

    /**
     * 根据用户名查询乘车人列表
     * @param username 用户名
     * @return 乘车人列表
     */
    List<PassengerRespDTO> listPassengerQueryByUsername(String username);

    /**
     * 根据乘车人ID集合查询乘车人列表
     * @param username 用户名
     * @param ids 乘车人ID集合
     * @return 乘车人列表
     */
    List<PassengerActualRespDTO> listPassengerQueryByIds(String username,List<Long> ids);

    /**
     * 新增乘车人
     * @param requestParam 乘车人信息
     */
    void savePassenger(PassengerReqDTO requestParam);

    /**
     * 修改乘车人
     * @param reqParam 乘车人信息
     */
    void updatePassenger(PassengerReqDTO reqParam);

    /**
     * 移除乘车人
     * @param reqParam 乘车人信息
     */
    void removePassenger(PassengerRemoveReqDTO reqParam);
}
