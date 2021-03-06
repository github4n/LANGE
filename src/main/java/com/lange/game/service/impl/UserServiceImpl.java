package com.lange.game.service.impl;/**
 * @Author linhao Hu
 * @Date 2020/6/24 0:03
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lange.game.domian.User;
import com.lange.game.mapper.UserMapper;
import com.lange.game.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @Author linhao Hu
 * @Date 2020/6/24 0:03
 *
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;
    @Override
    public User checkUser(User u) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getWxOpenid, u.getWxOpenid()));
        if(user == null){ //添加用户
            user = new User();
            user.setWxOpenid(u.getWxOpenid());
            user.setWxName(u.getWxName());
            user.setCreateTime(new Date());
            user.setGold(new BigDecimal(0));
            user.setPayGold(new BigDecimal(0));
            user.setWithdrawGold(new BigDecimal(0));
            user.setAvatarUrl(u.getAvatarUrl());
            userMapper.insert(user);
            log.info("用户添加成功，wxName:{};openId:{}",user.getWxName(),user.getWxOpenid());
        }else{
            log.info("用户查询成功，wxName:{};openId:{}",user.getWxName(),user.getWxOpenid());
        }
        return user;
    }

    @Override
    public User setName(Long userId, String name) {
        User u = userMapper.selectById(userId);
        if(u == null){
            return  null;
        }
        u.setName(name);
        u.setUpdateTime(new Date());
        userMapper.updateById(u);
        return u;
    }

}
