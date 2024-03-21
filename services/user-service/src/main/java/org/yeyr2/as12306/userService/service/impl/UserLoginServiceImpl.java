package org.yeyr2.as12306.userService.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.yeyr2.as12306.common.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yeyr2.as12306.bizs.user.core.UserContext;
import org.yeyr2.as12306.bizs.user.core.UserInfoDTO;
import org.yeyr2.as12306.bizs.user.toolkit.JWTUtil;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.convention.exception.ServiceException;
import org.yeyr2.as12306.designpattern.chain.AbstractChainContext;
import org.yeyr2.as12306.userService.common.constant.RedisKeyConstant;
import org.yeyr2.as12306.userService.common.enums.UserChainMarkEnum;
import org.yeyr2.as12306.userService.common.enums.UserRegisterErrorCodeEnum;
import org.yeyr2.as12306.userService.dao.entity.*;
import org.yeyr2.as12306.userService.dao.mapper.*;
import org.yeyr2.as12306.userService.dto.req.UserDeletionReqDTO;
import org.yeyr2.as12306.userService.dto.req.UserLoginReqDTO;
import org.yeyr2.as12306.userService.dto.req.UserRegisterReqDTO;
import org.yeyr2.as12306.userService.dto.resp.UserLoginRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserQueryRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserRegisterRespDTO;
import org.yeyr2.as12306.userService.service.UserLoginService;
import org.yeyr2.as12306.userService.service.UserService;
import org.yeyr2.as12306.userService.toolkit.UserReuseUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 用户登录接口实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserLoginServiceImpl implements UserLoginService {

    private UserService userService;
    private UserMapper userMapper;
    private UserReuseMapper userReuseMapper;
    private UserDeletionMapper userDeletionMapper;
    private UserPhoneMapper userPhoneMapper;
    private UserMailMapper userMailMapper;
    private RedissonClient redissonClient;
    private DistributedCache distributedCache;
    private AbstractChainContext<UserRegisterReqDTO> abstractChainContext;
    private RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO req) {
        String usernameOrMailOrPhone = req.getUsernameOrMailOrPhone();
        boolean mailFlag = false;
        for (char c : usernameOrMailOrPhone.toCharArray()){
            if(c == '@'){
                mailFlag = true;
                break;
            }
        }
        String username;
        if (mailFlag){
            LambdaQueryWrapper<UserMailDO> queryWrapper = Wrappers.lambdaQuery(UserMailDO.class)
                    .eq(UserMailDO::getMail,usernameOrMailOrPhone);
            username = Optional.ofNullable(userMailMapper.selectOne(queryWrapper))
                    .map(UserMailDO::getUsername)
                    .orElseThrow(() -> new ClientException("用户名/手机号/邮箱不存在"));
        }else{
            LambdaQueryWrapper<UserPhoneDO> queryWrapper =Wrappers.lambdaQuery(UserPhoneDO.class)
                    .eq(UserPhoneDO::getPhone, usernameOrMailOrPhone);
            username = Optional.ofNullable(userPhoneMapper.selectOne(queryWrapper))
                    .map(UserPhoneDO::getUsername)
                    .orElse(null);
        }
        username = Optional.ofNullable(username).orElse(req.getUsernameOrMailOrPhone());
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername,username)
                .eq(UserDO::getPassword, req.getPassword())
                .select(UserDO::getId,UserDO::getUsername, UserDO::getRealName);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        if(userDO != null){
            UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                    .userId(String.valueOf(userDO.getId()))
                    .username(userDO.getUsername())
                    .realName(userDO.getRealName())
                    .build();
            String accessToken = JWTUtil.generateAccessToken(userInfoDTO);
            UserLoginRespDTO actual = new UserLoginRespDTO(userInfoDTO.getUserId(), req.getUsernameOrMailOrPhone(),userDO.getRealName(),accessToken);
            distributedCache.put(accessToken, JSON.toJSONString(actual),30, TimeUnit.MINUTES);
            return actual;
        }
        throw new ServiceException("账号不存在或密码错误");
    }

    @Override
    public UserLoginRespDTO checkLogin(String accessToken) {
        return distributedCache.get(accessToken, UserLoginRespDTO.class);
    }

    @Override
    public void logout(String accessToken) {
        if(StrUtil.isNotBlank(accessToken)){
            distributedCache.delete(accessToken);
        }
    }

    @Override
    public Boolean hasUsername(String username) {
        // 布隆过滤器,存在用户名 的话再进行一次判断
        boolean hasUsername = userRegisterCachePenetrationBloomFilter.contains(username);
        if(hasUsername) {
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            return instance.opsForSet().isMember(RedisKeyConstant.USER_REGISTER_REUSE_SHARDING + UserReuseUtil.hashShardingUdx(username),username);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserRegisterRespDTO register(UserRegisterReqDTO registerParam) {
        abstractChainContext.handler(UserChainMarkEnum.USER_REGISTER_FILTER.name(),registerParam);
        RLock lock = redissonClient.getLock(RedisKeyConstant.LOCK_USER_REGISTER + registerParam.getUsername());
        boolean tryLock = lock.tryLock();
        if(!tryLock){
            throw new ServiceException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL);
        }
        try {
            try {
                int inserted = userMapper.insert(BeanUtil.convert(registerParam, UserDO.class));
                if(inserted < 1) {
                    throw new ServiceException(UserRegisterErrorCodeEnum.USER_REGISTER_FAIL);
                }
            }catch (DuplicateKeyException de){
                log.error("用户 [{}] 注册手机号 [{}] 重复", registerParam.getUsername(), registerParam.getPhone());
                throw new ServiceException(UserRegisterErrorCodeEnum.PHONE_REGISTERED);
            }
            if(StrUtil.isNotBlank(registerParam.getMail())){
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(registerParam.getMail())
                        .username(registerParam.getUsername())
                        .build();
                try {
                    userMailMapper.insert(userMailDO);
                }catch (DuplicateKeyException de){
                    log.error("用户 [{}] 注册邮箱 [{}] 重复", registerParam.getUsername(), registerParam.getMail());
                    throw new ServiceException(UserRegisterErrorCodeEnum.MAIL_REGISTERED);
                }
            }
            String username = registerParam.getUsername();
            userReuseMapper.delete(Wrappers.update(new UserReuseDO(username)));
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().remove(RedisKeyConstant.USER_REGISTER_REUSE_SHARDING + hasUsername(username),username);
            userRegisterCachePenetrationBloomFilter.add(username);
        }finally {
            lock.unlock();
        }
        return BeanUtil.convert(registerParam,UserRegisterRespDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletion(UserDeletionReqDTO requestParam) {
        String username = UserContext.getUsername();
        if(!Objects.equals(username,requestParam.getUsername())){
            // todo:应该上报风控中心检测
            throw new ClientException("注销账号与登录账号不一致.");
        }
        RLock lock = redissonClient.getLock(RedisKeyConstant.USER_DELETION + requestParam.getUsername());
        lock.lock();
        try{
            UserQueryRespDTO userQueryRespDTO = (UserQueryRespDTO) userService.queryUserByUsername(username,false);
            UserDeletionDO userDeletionDO = UserDeletionDO.builder()
                    .idType(userQueryRespDTO.getIdType())
                    .idCard(userQueryRespDTO.getIdCard())
                    .build();
            userDeletionMapper.insert(userDeletionDO);
            UserDO userDO = new UserDO();
            userDO.setDeletionTime(System.currentTimeMillis());
            userDO.setUsername(username);
            // MyBatis Plus 不支持修改语句变更 del_flag 字段
            userMapper.deletionUser(userDO);
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(userQueryRespDTO.getPhone())
                    .deletionTime(System.currentTimeMillis())
                    .build();
            userPhoneMapper.deletionUser(userPhoneDO);
            if(StrUtil.isNotBlank(userQueryRespDTO.getMail())){
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(userQueryRespDTO.getMail())
                        .deletionTime(System.currentTimeMillis())
                        .build();
                userMailMapper.deletionUser(userMailDO);
            }
            distributedCache.delete(UserContext.getToken());
            userReuseMapper.insert(new UserReuseDO(username));
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().add(RedisKeyConstant.USER_REGISTER_REUSE_SHARDING + UserReuseUtil.hashShardingUdx(username),username);
        }finally {
            lock.unlock();
        }
    }
}
