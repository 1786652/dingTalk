package com.example.dingtalk.job;

import com.aliyun.dingtalkworkflow_1_0.models.*;
import com.aliyun.tea.utils.StringUtils;
import com.aliyun.teautil.models.RuntimeOptions;
import com.example.dingtalk.config.RequestConfig;
import com.example.dingtalk.entity.AllTask;
import com.example.dingtalk.entity.Signet;
import com.example.dingtalk.entity.TitleTab;
import com.example.dingtalk.service.AllTaskService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
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

    @Autowired
    private TitleService titleService;

    @Autowired
    private SignetService signetService;

    private static final Logger logger = LogManager.getLogger(RefreshJob.class);

    private List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> resList = new ArrayList<>();

    private long now = System.currentTimeMillis();

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
            List<Signet> signetList = new ArrayList<>();
            Signet signetTask;
            for (GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult pullDown : formList) {
                String processCode = pullDown.getProcessCode();
                if ("PROC-02440ACA-59DC-425D-A510-ACC8FF08FD8D".equals(processCode)){
                    // 获取当前表单的所有审批单
                    List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult> instanceList = getInstanceList(processCode, 20L, 0L,null);
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
                        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks> tasks = task.getTasks();
                        StringJoiner stringJoiner = new StringJoiner(",");
                        for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultTasks task1 : tasks){
                            stringJoiner.add(task1.getUserId()+":"+task1.getResult());
                        }
                        signetTask.setTaskUsername(stringJoiner.toString());
                        String mobileUrl = tasks.get(0).getMobileUrl();
                        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultFormComponentValues> formComponentValues = task.getFormComponentValues();
                        List<GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultFormComponentValues> columnAndValues = formComponentValues;
                        for (GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResultFormComponentValues columnAndValue : columnAndValues) {
                            String name = columnAndValue.getName();
                            switch (name){
                                case "经办人":
                                    signetTask.setAgent(name + ":" +columnAndValue.getValue());
                                    break;
                                case "用章部门":
                                    signetTask.setSection(name + ":" +columnAndValue.getValue());
                                    break;
                                case "日期":
                                    signetTask.setDate(name + ":" + new SimpleDateFormat("yyyy-MM-dd").parse(columnAndValue.getValue()));
                                    break;
                                case "用章材料文件名称":
                                    signetTask.setMaterialFileName(name + ":" +columnAndValue.getValue());
                                    break;
                                case "文件份数":
                                    signetTask.setDocumentNum(name + ":" +Integer.parseInt(columnAndValue.getValue()));
                                    break;
                                case "文件类别":
                                    signetTask.setDocumentType(name + ":" +columnAndValue.getValue());
                                    break;
                                case "用章名称":
                                    signetTask.setSealName(name + ":" +columnAndValue.getValue());
                                    break;
                                case "用章类型":
                                    signetTask.setSealType(name + ":" +columnAndValue.getValue());
                                    break;
                                case "党章名称":
                                    signetTask.setChurchName(name + ":" +columnAndValue.getValue());
                                    break;
                                case "备注":
                                    signetTask.setRemark(name + ":" +columnAndValue.getValue());
                                    break;
                                case "附件":
                                    signetTask.setAttachments(name + ":" +columnAndValue.getValue());
                                    break;
                            }
                        }
                        signetList.add(signetTask);
                    }

                    signetService.saveOrUpdateBatch(signetList,100);
                }
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

        if(startTime == null){
                   /* DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // 定义日期格式
                    LocalDate localDate = LocalDate.parse("2021-01-01", formatter); // 解析日期字符串
                    startTime = localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(); // 转换为毫秒时间戳*/
                    startTime = now - 31449600000L;
                    listProcessInstanceIdsRequest.setProcessCode(processCode).setMaxResults(maxResults).setNextToken(nextToken).setStartTime(startTime);
                }else {
                    listProcessInstanceIdsRequest.setProcessCode(processCode).setMaxResults(maxResults).setNextToken(nextToken).setStartTime(startTime);
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
        System.out.println("startTime:" + new Date(startTime));
        System.out.println("endTime:" + new Date(endTime));
        System.out.println("nextToken1:" + nextToken1);
        for (String processInstanceId : list) {
            GetProcessInstanceResponseBody.GetProcessInstanceResponseBodyResult detail = getDetail(processInstanceId);
            resList.add(detail);
        }
        if(nextToken1 != null && !nextToken1.isEmpty()){
            getInstanceList(processCode,maxResults,Long.valueOf(nextToken1),startTime);
        }else if (nextToken1 == null && endTime < now){
            getInstanceList(processCode,maxResults,Long.valueOf(0L),endTime);
        }else{
            System.out.println("......");
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
}
