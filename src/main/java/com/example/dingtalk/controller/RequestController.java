package com.example.dingtalk.controller;

import com.aliyun.tea.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dingtalk.entity.Signet;
import com.example.dingtalk.entity.User;
import com.example.dingtalk.service.SampleInterface;
import com.example.dingtalk.service.SignetService;
import com.example.dingtalk.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class RequestController {
//    private final Map<String,SampleInterface> serviceMap;
//    @Autowired
//    public RequestController(Map<String, SampleInterface> serviceMap){
//        this.serviceMap = serviceMap;
//    }
    @Autowired
    private SignetService signetService;
    @Autowired
    private UserService userService;
    @RequestMapping("/getList")
    public Page<Signet> getList(@RequestParam(required = true)String processCode
            ,@RequestParam(required = true)String startTime
            ,@RequestParam(required = true)String finishTime
            ,@RequestParam(required = false)String originatorUserName
            ,@RequestParam(required = false)String businessId
            ,@RequestParam(required = true)Long size
            ,@RequestParam(required = true)Long page){

        QueryWrapper<Signet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("process_code",processCode)
                .between("create_time",startTime,finishTime);
        if(!StringUtils.isEmpty(originatorUserName)){
            QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.eq("user_name",originatorUserName);
            List<User> list = userService.list(userQueryWrapper);
            ArrayList<String> collect = list.stream().map(user -> user.getUserId()).collect(Collectors.toCollection(ArrayList::new));
            queryWrapper.in("originator_user_id",collect);
        }
        if(!StringUtils.isEmpty(businessId)){
            queryWrapper.eq("business_id",businessId);
        }
        Page<Signet> list = signetService.page(new Page<Signet>(page,size),queryWrapper);
        return list;
    }
}
