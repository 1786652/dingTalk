package com.example.dingtalk.job;

import com.aliyun.dingtalkoauth2_1_0.Client;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.dingtalkworkflow_1_0.models.*;
import com.aliyun.tea.utils.StringUtils;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.example.dingtalk.config.RequestConfig;
import com.example.dingtalk.entity.ApprovalCustomField;
import com.example.dingtalk.entity.Signet;
import com.example.dingtalk.entity.TitleTab;
import com.example.dingtalk.service.AllTaskService;
import com.example.dingtalk.service.ApprovalCustomFieldService;
import com.example.dingtalk.service.SignetService;
import com.example.dingtalk.service.TitleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RefreshJob2 implements Job {
    @Resource(name = "accessToken")
    private String accessToken;

    @Resource(name = "workClient")
    private com.aliyun.dingtalkworkflow_1_0.Client workClient;

    @Resource
    private RequestConfig requestConfig;

    @Autowired
    private TitleService titleService;

    @Autowired
    private SignetService signetService;

    @Autowired
    private ApprovalCustomFieldService customFieldService;

    private static final Logger logger = LogManager.getLogger(RefreshJob2.class);

    private List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> resList = new ArrayList<>();

    private long now = System.currentTimeMillis();

    private String depName;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            logger.debug("getPullDown Start");
            List<GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult> formList = getFormList();
            if(formList.size() == 0){
                logger.debug("getPullDown No Data");
                return;
            }
            TitleTab titleTab ;
            ArrayList<TitleTab> titleTabs = new ArrayList<>();
            for (GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult getManageProcessByStaffIdResponseBodyResult : formList) {
                titleTab = new TitleTab();
                BeanUtils.copyProperties(getManageProcessByStaffIdResponseBodyResult,titleTab);
                titleTabs.add(titleTab);
            }
            titleService.saveOrUpdateBatch(titleTabs);
            logger.debug("getPullDown End");
            List<Signet> signetList;
            Signet signetTask;
            for (GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult pullDown : formList) {
                String processCode = pullDown.getProcessCode();
                    signetList = new ArrayList<>();
                    // 获取当前表单的所有审批单
                    List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> instanceList = getInstanceList(processCode, 20L, 0L,null);
                    // 循环获取每个审批单的详情
                    for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult task : instanceList) {
                        signetTask = new Signet();
                        signetTask.setProcessCode(processCode);
                        signetTask.setBusinessId(task.getBusinessId());
                        signetTask.setTitle(task.getTitle());
                        signetTask.setCreateTime(sdf.parse(task.getCreateTime()));

                        if(!StringUtils.isEmpty(task.getFinishTime())){
                            signetTask.setFinishTime(sdf.parse(task.getFinishTime()));
                        }
                        else {
                            signetTask.setFinishTime(null);
                        }
                        signetTask.setOriginatorUserId(task.getOriginatorUserId());
                        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks> tasks = task.getTasks();
                        StringJoiner stringJoiner = new StringJoiner(",");
                        for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks task1 : tasks){
                            stringJoiner.add(task1.getUserId()+":"+task1.getResult());
                        }
                        String processInstanceId = "";
                        signetTask.setTaskUsername(stringJoiner.toString());
                        String mobileUrl = tasks.get(0).getMobileUrl();
                        int start = mobileUrl.indexOf("=") + 1; // 从 '=' 后面开始截取
                        int end = mobileUrl.indexOf("&");
                        // 截取子字符串
                        if (start != -1 && end != -1 && start < end) {
                            processInstanceId = mobileUrl.substring(start, end);
                        }
                        signetTask.setProcessInstanceId(processInstanceId);
                        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultFormComponentValues> formComponentValues = task.getFormComponentValues();
                        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultFormComponentValues> columnAndValues = formComponentValues;
                        String finalProcessInstanceId = processInstanceId;
                        ArrayList<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultFormComponentValues> collect2 = columnAndValues
                                .stream().filter(o -> !StringUtils.isEmpty(o.getValue())).collect(Collectors.toCollection(ArrayList::new));
                        ArrayList<ApprovalCustomField> collect1 = collect2.stream().map(o -> {
                            ApprovalCustomField approvalCustomField = new ApprovalCustomField();
                            approvalCustomField.setFieldName(o.getName());
                            approvalCustomField.setFieldValue(o.getValue());
                            approvalCustomField.setProcessInstanceId(finalProcessInstanceId);
                            approvalCustomField.setId(o.getId()+finalProcessInstanceId);
                            return approvalCustomField;
                        }).collect(Collectors.toCollection(ArrayList::new));
                        customFieldService.saveOrUpdateBatch(collect1);
                        StringJoiner sj = new StringJoiner(",");
                        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultOperationRecords> operationRecords = task.getOperationRecords();
                        ArrayList<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultOperationRecords> collect = operationRecords.stream().filter(o ->
                                {
                                    return !StringUtils.isEmpty(o.getCcUserIds());
                                }).collect(Collectors.toCollection(ArrayList::new));
                        for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultOperationRecords operationRecord : collect) {
                            operationRecord.getCcUserIds().forEach(sj::add);
                        }
                        signetTask.setCcUserIds(sj.toString());
                        sj = new StringJoiner(",");
                        ArrayList<String> flows = operationRecords.stream().map(o -> o.getShowName() + ":" + o.getUserId()).collect(Collectors.toCollection(ArrayList::new));
                        flows.forEach(sj::add);
                        signetTask.setFlow(sj.toString());
                        signetTask.setDepName(depName);
                        signetList.add(signetTask);
                    }
                    signetService.saveOrUpdateBatch(signetList,100);
                accessToken = accessToken();
            }
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
    private List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> getInstanceList(String processCode, Long maxResults, Long nextToken,Long startTime) throws Exception {
        ListProcessInstanceIdsHeaders listProcessInstanceIdsHeaders = new ListProcessInstanceIdsHeaders();
        listProcessInstanceIdsHeaders.setXAcsDingtalkAccessToken(accessToken);
        ListProcessInstanceIdsRequest listProcessInstanceIdsRequest = new ListProcessInstanceIdsRequest();
        ArrayList<String> status = new ArrayList<>();
        status.add("COMPLETED");
        if(startTime == null){
                    startTime = now - 31449600000L;
            listProcessInstanceIdsRequest.setProcessCode(processCode).setMaxResults(maxResults).setNextToken(nextToken).setStartTime(startTime).setStatuses(status);
                }else {
                    listProcessInstanceIdsRequest.setProcessCode(processCode).setMaxResults(maxResults).setNextToken(nextToken).setStartTime(startTime).setStatuses(status);
                }
        long endTime = Math.min(startTime + 10368000000L, now);
        if(startTime < endTime){
            listProcessInstanceIdsRequest.setEndTime(endTime);
        }else {
            endTime = now;
        }
        ListProcessInstanceIdsResponse listProcessInstanceIdsResponse = workClient.listProcessInstanceIdsWithOptions(listProcessInstanceIdsRequest, listProcessInstanceIdsHeaders, new RuntimeOptions());
        List<String> list = listProcessInstanceIdsResponse.getBody().getResult().getList();
        String nextToken1 = listProcessInstanceIdsResponse.getBody().getResult().getNextToken();
        for (String processInstanceId : list) {
            GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult detail = getDetail(processInstanceId);
            resList.add(detail);
        }
        if(nextToken1 != null && !nextToken1.isEmpty()){
            getInstanceList(processCode,maxResults,Long.valueOf(nextToken1),startTime);
        }else if (nextToken1 == null && endTime < now){
            getInstanceList(processCode,maxResults,Long.valueOf(0L),endTime);
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
        depName = result.getOriginatorDeptName();
        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks> tasks = result.getTasks();
//        for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultOperationRecords operationRecord : operationRecords) {
//            operationRecord.setUserId(operationRecord.getUserId()+":"+getUser(operationRecord.getUserId()));
//        }
//        for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks task : tasks) {
//            task.setUserId(task.getUserId()+":"+getUser(task.getUserId()));
//        }
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
        String name = null;
        try {
             name = m.get("name");
        }catch (Exception e ){
            return "未找到审批人";
        }

        return name;
    }

    public String accessToken() throws Exception {
        Client client = new Client(new Config().setProtocol("https").setRegionId("central"));
        GetAccessTokenResponse accessTokenResponse = client.getAccessToken(new GetAccessTokenRequest()
                .setAppKey("dingb1cmlolkaqutjzk1")
                .setAppSecret("hmQuT_VRkQwsw7fyMfrF4gAVIArkL8NhjGoXCc42wdfqN8TmyIrqK8mt5nyQxYFn"));
        accessToken = accessTokenResponse.getBody().getAccessToken();
        return accessToken;
    }
}
