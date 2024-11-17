package com.example.dingtalk.controller;

import com.aliyun.dingtalkoauth2_1_0.Client;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.dingtalkworkflow_1_0.models.GrantProcessInstanceForDownloadFileHeaders;
import com.aliyun.dingtalkworkflow_1_0.models.GrantProcessInstanceForDownloadFileRequest;
import com.aliyun.dingtalkworkflow_1_0.models.GrantProcessInstanceForDownloadFileResponse;
import com.aliyun.tea.utils.StringUtils;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dingtalk.config.RequestConfig;
import com.example.dingtalk.entity.*;
import com.example.dingtalk.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@RestController
public class RequestController {
    @Autowired
    private SignetService signetService;
    @Autowired
    private UserService userService;
    @Autowired
    private ApprovalCustomFieldService approvalCustomFieldService;
    @Autowired
    private TitleService titleService;
    @Autowired
    private RequestConfig requestConfig;
    @Resource(name = "workClient")
    private com.aliyun.dingtalkworkflow_1_0.Client workClient;
    @RequestMapping("/getPullDown")
    public List<TitleTab> getPullDown(){
        return titleService.list();
    }
    @RequestMapping("/getList")
    public Page<Signet> getList(@RequestParam(required = true)String processCode
            ,@RequestParam(required = true)String startTime
            ,@RequestParam(required = true)String finishTime
            ,@RequestParam(required = false)String originatorUserName
            ,@RequestParam(required = false)String businessId
            ,@RequestParam(required = true)Long size
            ,@RequestParam(required = true)Long page) throws Exception {
        // 解析字符串为 LocalDate
        LocalDate localDate = LocalDate.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endTime = LocalDate.parse(finishTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // 转换为 LocalDateTime，时间设置为 0 点 0 分 0 秒
        LocalDateTime start = localDate.atStartOfDay();
        LocalDateTime end = endTime.atTime(23, 59, 59);
        // 格式化为 yyyy-MM-dd HH:mm:ss 格式
        String startTimeStr = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String finishTimeStr = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        QueryWrapper<Signet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("process_code",processCode)
                .between("create_time",startTimeStr,finishTimeStr);
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
        StringJoiner sj;
        for (Signet record : list.getRecords()) {
            sj = new StringJoiner(",");
            String[] split = record.getTaskUsername().split(",");
            for (String s : split) {
                if(!StringUtils.isEmpty(s)){
                    User userId = userService.getOne(new QueryWrapper<User>().eq("user_id", s));
                    String userName;
                    if(!StringUtils.isEmpty(userId)){
                        userName = userId.getUserName();
                    }else{
                        userName = "";
                    }
                    sj.add(userName);
                }
            }
            record.setTaskUsername(sj.toString());
        }
        return list;
    }
    @RequestMapping("/getFormDetail")
    @ResponseBody
    public Signet getDetail(@RequestParam String processCode,@RequestParam String businessId,@RequestParam String processInstanceId){
        QueryWrapper<ApprovalCustomField> approvalCustomFieldQueryWrapper = new QueryWrapper<>();
        approvalCustomFieldQueryWrapper.eq("process_instance_id",processInstanceId)
                .eq("process_code",processCode)
                .eq("business_id",businessId).orderByAsc("id");
        List<ApprovalCustomField> list = approvalCustomFieldService.list(approvalCustomFieldQueryWrapper);
        Signet signet = signetService.getOne(new QueryWrapper<Signet>().eq("process_instance_id", processInstanceId).eq("process_code",processCode)
                .eq("business_id",businessId));
        signet.setApprovalCustomFieldList(list);
        // 获取审批人名称
        String taskUsername = signet.getTaskUsername();
        String[] split = taskUsername.split(",");
        for (String s : split) {
            if(!StringUtils.isEmpty(s)){
                User userId = userService.getOne(new QueryWrapper<>(User.class).eq("user_id", s));
                String userName;
                if(!StringUtils.isEmpty(userId)){
                    userName = userId.getUserName();
                }else{
                    userName = "";
                }

                taskUsername = taskUsername.replace(s,userName);
            }
        }
        signet.setTaskUsername(taskUsername);
        // 获取抄送人
        String ccUserIds = signet.getCcUserIds();
        if(!StringUtils.isEmpty(ccUserIds)){
            String[] split1 = ccUserIds.split(",");
            for (String s : split1) {
                if(!StringUtils.isEmpty(s)){
                    String userName = userService.getOne(new QueryWrapper<>(User.class).eq("user_id", s)).getUserName();
                    ccUserIds = ccUserIds.replace(s,userName);
                }
            }
            signet.setCcUserIds(ccUserIds);
        }
        // 获取发起人名称
        String originatorUserId = signet.getOriginatorUserId();
        if(!StringUtils.isEmpty(originatorUserId)){
            String userName = userService.getOne(new QueryWrapper<>(User.class).eq("user_id", originatorUserId)).getUserName();
            originatorUserId = userName;
        }
        signet.setOriginatorUserId(originatorUserId);
        // 获取流程
        String flow = signet.getFlow();
        if(!StringUtils.isEmpty(flow)){
            String[] split1 = flow.split(",");
            for (String s : split1) {
                if(!StringUtils.isEmpty(s)){
                    String s1 = s.split(":")[1];
                    String userName = userService.getOne(new QueryWrapper<>(User.class).eq("user_id", s1)).getUserName();
                    flow = flow.replace(s1,userName);
                }
            }
            signet.setFlow(flow);
        }
        return signet;
    }

    @RequestMapping("/getFile")
    public GrantProcessInstanceForDownloadFileResponse getFile(String processInstanceId, String fileId) throws Exception {

        GrantProcessInstanceForDownloadFileHeaders grantProcessInstanceForDownloadFileHeaders = new GrantProcessInstanceForDownloadFileHeaders();
        grantProcessInstanceForDownloadFileHeaders.setXAcsDingtalkAccessToken(accessToken());
        GrantProcessInstanceForDownloadFileRequest grantProcessInstanceForDownloadFileRequest = new GrantProcessInstanceForDownloadFileRequest()
                .setProcessInstanceId(processInstanceId)
                .setFileId(fileId).setWithCommentAttatchment(false);
        GrantProcessInstanceForDownloadFileResponse grantProcessInstanceForDownloadFileResponse = workClient
                .grantProcessInstanceForDownloadFileWithOptions(grantProcessInstanceForDownloadFileRequest, grantProcessInstanceForDownloadFileHeaders, new RuntimeOptions());
        return grantProcessInstanceForDownloadFileResponse;
    }

    private String accessToken() throws Exception {
        Client client = new Client(new Config().setProtocol("https").setRegionId("central"));
        GetAccessTokenResponse accessTokenResponse = client.getAccessToken(new GetAccessTokenRequest()
                .setAppKey("dingb1cmlolkaqutjzk1")
                .setAppSecret("hmQuT_VRkQwsw7fyMfrF4gAVIArkL8NhjGoXCc42wdfqN8TmyIrqK8mt5nyQxYFn"));
        String accessToken = accessTokenResponse.getBody().getAccessToken();
        return accessToken;
    }

}
