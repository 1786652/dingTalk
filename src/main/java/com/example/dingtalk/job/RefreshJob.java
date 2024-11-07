package com.example.dingtalk.job;

import com.aliyun.dingtalkworkflow_1_0.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import com.example.dingtalk.config.RequestConfig;
import com.example.dingtalk.entity.AllTask;
import com.example.dingtalk.service.AllTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.text.SimpleDateFormat;
import java.util.*;
@Component
public class RefreshJob implements Job {
    @Resource(name = "accessToken")
    private String accessToken;

    @Resource(name = "workClient")
    private com.aliyun.dingtalkworkflow_1_0.Client workClient;

    @Resource
    private RequestConfig requestConfig;

    @Autowired
    private AllTaskService allTaskService;

    private static final Logger logger = LogManager.getLogger(RefreshJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        try {
            logger.debug("getPullDown Start");
            List<GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult> formList = getFormList();
            if(formList.size() == 0){
                logger.debug("getPullDown No Data");
                return;
            }
            logger.debug("getPullDown End");
            List<AllTask> allTasks = new ArrayList<>();
            AllTask allTask = new AllTask();
            for (GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult pullDown : formList) {
                String processCode = pullDown.getProcessCode();
                // 获取当前公司的所有实例
                List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> instanceList = getInstanceList(processCode, 10L, null);
                for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult task : instanceList) {
                    allTask = new AllTask();
                    allTask.setProcessCode(processCode);
                    allTask.setBusinessId(task.getBusinessId());
                    allTask.setTitle(task.getTitle());
                    allTask.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(task.getCreateTime()));
                    if(!task.getFinishTime().isEmpty()){
                        allTask.setFinishTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(task.getFinishTime()));
                    }
                    else {
                        allTask.setFinishTime("");
                    }
                    List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks> tasks = task.getTasks();
                    StringJoiner stringJoiner = new StringJoiner(",");
                    for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks task1 : tasks){
                        String user = getUser(task1.getUserId());
                        stringJoiner.add(user);
                    }
                    allTask.setUsers(stringJoiner.toString());
                    allTasks.add(allTask);
                }
            }
            allTaskService.saveBatch(allTasks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.debug("RefreshJob End");
    }
    // 获取下拉所有公司
    private List<GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult> getFormList() throws Exception {
        GetManageProcessByStaffIdRequest getManageProcessByStaffIdRequest = new GetManageProcessByStaffIdRequest();
        GetManageProcessByStaffIdResponse manageProcessByStaffIdWithOptions = workClient
                .getManageProcessByStaffIdWithOptions(getManageProcessByStaffIdRequest.setUserId("265502034527563439")
                        , new GetManageProcessByStaffIdHeaders().setXAcsDingtalkAccessToken(accessToken)
                        ,new RuntimeOptions());
        return manageProcessByStaffIdWithOptions.getBody().getResult();
    }
    // 获取实例list
    private List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> getInstanceList(String processCode, Long maxResults, Long nextToken) throws Exception {
        ListProcessInstanceIdsHeaders listProcessInstanceIdsHeaders = new ListProcessInstanceIdsHeaders();
        listProcessInstanceIdsHeaders.setXAcsDingtalkAccessToken(accessToken);
        ListProcessInstanceIdsRequest listProcessInstanceIdsRequest = new ListProcessInstanceIdsRequest();
        listProcessInstanceIdsRequest.setProcessCode(processCode).setMaxResults(maxResults).setNextToken(nextToken)
                .setStartTime(System.currentTimeMillis());
        ListProcessInstanceIdsResponse listProcessInstanceIdsResponse = workClient.listProcessInstanceIdsWithOptions(listProcessInstanceIdsRequest, listProcessInstanceIdsHeaders, new RuntimeOptions());
        List<String> list = listProcessInstanceIdsResponse.getBody().getResult().getList();
        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> resList = new ArrayList<>();
        for (String s : list) {
            GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult detail = getDetail(s);
            resList.add(detail);
        }
        return resList;
    }
    // 获取明细
    private GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult getDetail(String processInstanceId) throws Exception {
        GetProcessInstanceHeaders processInstanceHeaders = new GetProcessInstanceHeaders();
        processInstanceHeaders.setXAcsDingtalkAccessToken(accessToken);
        GetProcessInstanceRequest processInstanceRequest = new GetProcessInstanceRequest();
        processInstanceRequest.setProcessInstanceId(processInstanceId);
        GetProcessInstanceResponse processInstanceWithOptions = workClient.getProcessInstanceWithOptions(processInstanceRequest, processInstanceHeaders, new RuntimeOptions());
        GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult result = processInstanceWithOptions.body.getResult();
        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultOperationRecords> operationRecords = result.getOperationRecords();
        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks> tasks = result.getTasks();
        for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultOperationRecords operationRecord : operationRecords) {
            operationRecord.setUserId(operationRecord.getUserId()+":"+getUser(operationRecord.getUserId()));
        }
        for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks task : tasks) {
            task.setUserId(task.getUserId()+":"+getUser(task.getUserId()));
        }
        return result;
    }
    private String getUser(String userId) throws Exception {
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
}
