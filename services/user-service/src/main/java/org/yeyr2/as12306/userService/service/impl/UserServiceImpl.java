package org.yeyr2.as12306.userService.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.yeyr2.as12306.common.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.userService.dao.entity.UserDO;
import org.yeyr2.as12306.userService.dao.entity.UserDeletionDO;
import org.yeyr2.as12306.userService.dao.entity.UserMailDO;
import org.yeyr2.as12306.userService.dao.mapper.UserDeletionMapper;
import org.yeyr2.as12306.userService.dao.mapper.UserMailMapper;
import org.yeyr2.as12306.userService.dao.mapper.UserMapper;
import org.yeyr2.as12306.userService.dto.req.UserUpdateReqDTO;
import org.yeyr2.as12306.userService.dto.resp.UserQueryActualRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserQueryRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserRespDTO;
import org.yeyr2.as12306.userService.service.UserService;

import java.util.Objects;
import java.util.Optional;

/**
 * 用户信息接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private UserMapper userMapper;
    private UserDeletionMapper userDeletionMapper;
    private UserMailMapper userMailMapper;

    @Override
    public UserRespDTO queryUserByUserId(String userId, Boolean isActual) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers
                .lambdaQuery(UserDO.class)
                .eq(UserDO::getId, userId);
        UserDO user = userMapper.selectOne(wrapper);
        if(user == null){
            throw new ClientException("用户不存在,请检查用户ID是否正确");
        }
        return isActual ? BeanUtil.convert(user, UserQueryActualRespDTO.class) : BeanUtil.convert(user, UserQueryRespDTO.class);
    }

    @Override
    public UserRespDTO queryUserByUsername(String username, Boolean isActual) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers
                .lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername,username);
        UserDO user = userMapper.selectOne(wrapper);
        if(user == null){
            throw new ClientException("用户不存在,请检查用户名是否正确");
        }
        return isActual ? BeanUtil.convert(user, UserQueryActualRespDTO.class) : BeanUtil.convert(user, UserQueryRespDTO.class);
    }

    @Override
    public Long queryUserDeletionNum(Integer idType, String idCard) {
        LambdaQueryWrapper<UserDeletionDO> wrapper = Wrappers
                .lambdaQuery(UserDeletionDO.class)
                .eq(UserDeletionDO::getIdType,idType)
                .eq(UserDeletionDO::getIdCard,idCard);
        // todo: 先查缓存
        Long deletionCount = userDeletionMapper.selectCount(wrapper);
        return Optional.ofNullable(deletionCount).orElse(0L);
    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        UserQueryRespDTO userQueryRespDTO = (UserQueryRespDTO) queryUserByUsername(requestParam.getUsername(),false);
        UserDO user = BeanUtil.convert(requestParam, UserDO.class);
        LambdaUpdateWrapper<UserDO> userUpdateWrapper = Wrappers
                .lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername,requestParam.getUsername());
        userMapper.update(user, userUpdateWrapper);
        // 查看是否更新mail,如果更新,直接删除并添加新的mail数据
        if(StrUtil.isNotBlank(requestParam.getMail()) && !Objects.equals(requestParam.getMail(), userQueryRespDTO.getMail())){
            LambdaUpdateWrapper<UserMailDO> mailUpdateWrapper = Wrappers
                    .lambdaUpdate(UserMailDO.class)
                    .eq(UserMailDO::getMail, userQueryRespDTO.getMail());
            userMailMapper.delete(mailUpdateWrapper);
            UserMailDO userMail = UserMailDO.builder()
                    .mail(requestParam.getMail())
                    .username(requestParam.getUsername())
                    .build();
            userMailMapper.insert(userMail);
        }
    }
}
