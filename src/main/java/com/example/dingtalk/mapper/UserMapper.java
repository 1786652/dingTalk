package com.example.dingtalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingtalk.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
