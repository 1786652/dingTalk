package com.example.dingtalk.controller;

import com.aliyun.dingtalkoauth2_1_0.Client;
import com.aliyun.dingtalkworkflow_1_0.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import com.example.dingtalk.config.RequestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class TestController {

    @Autowired
    private RequestConfig requestConfig;

    @Resource(name = "accessToken")
    private String accessToken;

    @Resource(name = "workClient")
    private com.aliyun.dingtalkworkflow_1_0.Client workClient;

    @Autowired
    private Client client;

    // 获取单个审批实例详情
    @RequestMapping("/getDetail")
    public GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult getDetail(String processInstanceId) throws Exception {
        GetProcessInstanceHeaders processInstanceHeaders = new GetProcessInstanceHeaders();
        processInstanceHeaders.setXAcsDingtalkAccessToken(accessToken);
        GetProcessInstanceRequest processInstanceRequest = new GetProcessInstanceRequest();
        processInstanceRequest.setProcessInstanceId(processInstanceId);
        GetProcessInstanceResponse processInstanceWithOptions = workClient.getProcessInstanceWithOptions(processInstanceRequest, processInstanceHeaders, new RuntimeOptions());
        GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult result = processInstanceWithOptions.body.getResult();

        return result;
    }

    // 审批流程
//    @RequestMapping("/test")
//    public ListUserVisibleBpmsProcessesResponse test(Long maxResults, Long nextToken) throws Exception {
//        // 获取审批模板列表请求头
//        ListUserVisibleBpmsProcessesHeaders querySchemaByProcessCodeHeaders = new ListUserVisibleBpmsProcessesHeaders();
//        querySchemaByProcessCodeHeaders.setXAcsDingtalkAccessToken(accessToken);
//        // 获取审批模板列表请求参数
//        ListUserVisibleBpmsProcessesRequest querySchemaByProcessCodeRequest = new ListUserVisibleBpmsProcessesRequest();
//        querySchemaByProcessCodeRequest.setMaxResults(maxResults).setNextToken(nextToken);
//        ListUserVisibleBpmsProcessesResponse listUserVisibleBpmsProcessesResponse = workClient.listUserVisibleBpmsProcessesWithOptions(querySchemaByProcessCodeRequest, querySchemaByProcessCodeHeaders, new RuntimeOptions());
//        return listUserVisibleBpmsProcessesResponse;
//    }

    // 获取所有实例列表
    @RequestMapping("/getInstanceList")
    public ListProcessInstanceIdsResponse getInstanceList(String processCode,String startTime,String endTime,Long maxResults, Long nextToken) throws Exception {
        ListProcessInstanceIdsHeaders listProcessInstanceIdsHeaders = new ListProcessInstanceIdsHeaders();
        listProcessInstanceIdsHeaders.setXAcsDingtalkAccessToken(accessToken);
        ListProcessInstanceIdsRequest listProcessInstanceIdsRequest = new ListProcessInstanceIdsRequest();
        listProcessInstanceIdsRequest.setProcessCode(processCode).setMaxResults(maxResults).setNextToken(nextToken)
                .setStartTime(new SimpleDateFormat("yyyy-MM-dd").parse(startTime).getTime());
                if(endTime != null){
                    listProcessInstanceIdsRequest.setEndTime(new SimpleDateFormat("yyyy-MM-dd").parse(endTime).getTime());
                }
        ListProcessInstanceIdsResponse listProcessInstanceIdsResponse = workClient.listProcessInstanceIdsWithOptions(listProcessInstanceIdsRequest, listProcessInstanceIdsHeaders, new RuntimeOptions());
//        List<String> list = listProcessInstanceIdsResponse.getBody().getResult().getList();
//        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> resList = new ArrayList<>();
//        for (String s : list) {
//            GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult detail = getDetail(s);
//            resList.add(detail);
//        }
//
//        return resList;
        return listProcessInstanceIdsResponse;
    }
    // 获取用户信息
    @RequestMapping("/getUser")
    public String getUser(String userId) throws Exception {
        Map<String,String> reqMap = new HashMap<>();
        // 构建请求参数
        reqMap.put("language","zh_CN");
        reqMap.put("userid",userId);
        ObjectMapper objectMapper = new ObjectMapper();
        String req = objectMapper.writeValueAsString(reqMap);
        // 发送请求
        String res = requestConfig.doPost("https://oapi.dingtalk.com/topapi/v2/user/get?access_token=" + accessToken, req);
        Map<String,Object> map = objectMapper.readValue(res, Map.class);
        Map<String,String> m = (Map<String,String>)map.get("result");
        // 解析出名字
        String name = m.get("name");
        return name;
    }

    @RequestMapping("/getFile")
    public GrantProcessInstanceForDownloadFileResponse getFile() throws Exception {

        GrantProcessInstanceForDownloadFileHeaders grantProcessInstanceForDownloadFileHeaders = new GrantProcessInstanceForDownloadFileHeaders();
        grantProcessInstanceForDownloadFileHeaders.setXAcsDingtalkAccessToken(accessToken);
        GrantProcessInstanceForDownloadFileRequest grantProcessInstanceForDownloadFileRequest = new GrantProcessInstanceForDownloadFileRequest()
                .setProcessInstanceId("0jLCTlw-QRqOmOksYcfK-g02781730184146")
                .setFileId("158062672325").setWithCommentAttatchment(false);
        GrantProcessInstanceForDownloadFileResponse grantProcessInstanceForDownloadFileResponse = workClient
                .grantProcessInstanceForDownloadFileWithOptions(grantProcessInstanceForDownloadFileRequest, grantProcessInstanceForDownloadFileHeaders, new RuntimeOptions());
        return grantProcessInstanceForDownloadFileResponse;
    }
    // TODO:获取USERID获取不到
    @RequestMapping("/getUserId")
    public String getUserId() throws Exception {
        String res = requestConfig
                .doPost("https://oapi.dingtalk.com/topapi/v2/user/getuserinfo?access_token=" + accessToken,"");
    return res;
    }

    @RequestMapping("/getFormList")
    public List<GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult> getFormList() throws Exception {
        GetManageProcessByStaffIdRequest getManageProcessByStaffIdRequest = new GetManageProcessByStaffIdRequest();
        GetManageProcessByStaffIdResponse manageProcessByStaffIdWithOptions = workClient
                .getManageProcessByStaffIdWithOptions(getManageProcessByStaffIdRequest.setUserId("265502034527563439")
                        , new GetManageProcessByStaffIdHeaders().setXAcsDingtalkAccessToken(accessToken)
                        ,new RuntimeOptions());
        return manageProcessByStaffIdWithOptions.getBody().getResult();
    }
}
