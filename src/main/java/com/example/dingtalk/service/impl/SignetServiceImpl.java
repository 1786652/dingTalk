package com.example.dingtalk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dingtalk.entity.Signet;
import com.example.dingtalk.mapper.SignetMapper;
import com.example.dingtalk.service.SampleInterface;
import com.example.dingtalk.service.SignetService;
import org.springframework.stereotype.Service;

@Service("PROC-02440ACA-59DC-425D-A510-ACC8FF08FD8D")
public class SignetServiceImpl extends ServiceImpl<SignetMapper, Signet> implements SignetService , SampleInterface {
}
