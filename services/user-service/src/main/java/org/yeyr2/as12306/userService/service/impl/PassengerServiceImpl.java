package org.yeyr2.as12306.userService.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.yeyr2.as12306.common.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.yeyr2.as12306.bizs.user.core.UserContext;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.convention.exception.ServiceException;
import org.yeyr2.as12306.userService.common.constant.RedisKeyConstant;
import org.yeyr2.as12306.userService.common.enums.VerifyStatusEnum;
import org.yeyr2.as12306.userService.dao.entity.PassengerDO;
import org.yeyr2.as12306.userService.dao.mapper.PassengerMapper;
import org.yeyr2.as12306.userService.dto.req.PassengerRemoveReqDTO;
import org.yeyr2.as12306.userService.dto.req.PassengerReqDTO;
import org.yeyr2.as12306.userService.dto.resp.PassengerActualRespDTO;
import org.yeyr2.as12306.userService.dto.resp.PassengerRespDTO;
import org.yeyr2.as12306.userService.service.PassengerService;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private PassengerMapper passengerMapper;
    private PlatformTransactionManager transactionManager;
    private DistributedCache distributedCache;

    @Override
    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);
        return Optional.ofNullable(actualUserPassengerListStr)
                .map(each -> JSON.parseArray(each, PassengerDO.class))
                .map(each -> BeanUtil.convert(each,PassengerRespDTO.class))
                .orElse(null);
    }

    private String getActualUserPassengerListStr(String username){
        return distributedCache.safeGet(
                RedisKeyConstant.USER_PASSENGER_LIST + username,
                String.class,
                () -> {
                    LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername,username);
                    List<PassengerDO> passengerDOS = passengerMapper.selectList(queryWrapper);
                    return CollUtil.isNotEmpty(passengerDOS) ? JSON.toJSONString(passengerDOS) : null;
                },
                1,
                TimeUnit.DAYS
        );
    }

    @Override
    public List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);
        if(StrUtil.isEmpty(actualUserPassengerListStr)){
            return null;
        }
        return JSON.parseArray(actualUserPassengerListStr,PassengerDO.class)
                .stream().filter(passengerDO -> ids.contains(passengerDO.getId()))
                .map(each -> BeanUtil.convert(each, PassengerActualRespDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        TransactionStatus transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        String username = UserContext.getUsername();
        try{
            PassengerDO passengerDO = BeanUtil.convert(requestParam,PassengerDO.class);
            passengerDO.setUsername(username);
            passengerDO.setCreateDate(new Date());
            passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());
            int inserted = passengerMapper.insert(passengerDO);
            if(!SqlHelper.retBool(inserted)){
                throw new ServiceException(String.format("[%s] 新增乘车人失败",username));
            }
        }catch (Exception ex){
            if(ex instanceof ServiceException){
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 新增乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            transactionManager.rollback(transactionStatus);
            throw ex;
        }
        delUserPassengerCache(username);
    }

    private void delUserPassengerCache(String username){
        distributedCache.delete(RedisKeyConstant.USER_PASSENGER_LIST + username);
    }

    @Override
    public void updatePassenger(PassengerReqDTO reqParam) {
        TransactionStatus transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        String username = UserContext.getUsername();
        try{
            PassengerDO passengerDO = BeanUtil.convert(reqParam, PassengerDO.class);
            passengerDO.setUsername(username);
            LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername,username)
                    .eq(PassengerDO::getId, reqParam.getId());
            int updated = passengerMapper.update(passengerDO,updateWrapper);
            if(!SqlHelper.retBool(updated)){
                throw new ServiceException(String.format("[%s] 修改乘车人失败", username));
            }
            transactionManager.commit(transactionStatus);
        } catch (Exception ex) {
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(reqParam));
            } else {
                log.error("[{}] 修改乘车人失败，请求参数：{}", username, JSON.toJSONString(reqParam), ex);
            }
            transactionManager.rollback(transactionStatus);
            throw ex;
        }
        delUserPassengerCache(username);
    }

    @Override
    public void removePassenger(PassengerRemoveReqDTO reqParam) {
        TransactionStatus transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        String username = UserContext.getUsername();
        PassengerDO passengerDO = selectPassenger(username,reqParam.getId());
        if(Objects.isNull(passengerDO)){
            throw new ClientException("乘车人数据不存在");
        }
        try{
            LambdaUpdateWrapper<PassengerDO> deleteWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername,username)
                    .eq(PassengerDO::getId,reqParam.getId());
            int deleted = passengerMapper.delete(deleteWrapper);
            if(!SqlHelper.retBool(deleted)){
                throw new ServiceException(String.format("[%s] 删除乘车人失败",username));
            }
            transactionManager.commit(transactionStatus);
        }catch (Exception ex){
            if(ex instanceof ServiceException){
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(reqParam));
            }else{
                log.error("[{}] 删除乘车人失败，请求参数：{}", username, JSON.toJSONString(reqParam), ex);
            }
            transactionManager.rollback(transactionStatus);
            throw ex;
        }
        delUserPassengerCache(username);
    }

    private PassengerDO selectPassenger(String username,String passengerId){
        LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername,username)
                .eq(PassengerDO::getId,passengerId);
        return passengerMapper.selectOne(queryWrapper);
    }
}
