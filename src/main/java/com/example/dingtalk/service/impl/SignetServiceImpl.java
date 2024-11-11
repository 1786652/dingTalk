package com.example.dingtalk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dingtalk.entity.Signet;
import com.example.dingtalk.mapper.SignetMapper;
import com.example.dingtalk.service.SignetService;
import org.springframework.stereotype.Service;

@Service
public class SignetServiceImpl extends ServiceImpl<SignetMapper, Signet> implements SignetService {
}
