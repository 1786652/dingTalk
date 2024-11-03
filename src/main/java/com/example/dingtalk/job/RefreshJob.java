package com.example.dingtalk.job;

import com.aliyun.dingtalkworkflow_1_0.models.GetManageProcessByStaffIdHeaders;
import com.aliyun.dingtalkworkflow_1_0.models.GetManageProcessByStaffIdRequest;
import com.aliyun.dingtalkworkflow_1_0.models.GetManageProcessByStaffIdResponse;
import com.aliyun.dingtalkworkflow_1_0.models.GetManageProcessByStaffIdResponseBody;
import com.aliyun.teautil.models.RuntimeOptions;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;



import java.util.List;

public class RefreshJob implements Job {
    @Resource(name = "accessToken")
    private String accessToken;

    @Resource(name = "workClient")
    private com.aliyun.dingtalkworkflow_1_0.Client workClient;

    private static final Logger logger = LogManager.getLogger(RefreshJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("This is an INFO message");
        logger.warn("This is a WARN message");
        logger.error("This is an ERROR message");
        logger.debug("RefreshJob Start");
        try {
            logger.debug("getPullDown Start");
            List<GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult> formList = getFormList();
            logger.debug("getPullDown End");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.debug("RefreshJob End");
    }
    public List<GetManageProcessByStaffIdResponseBody.GetManageProcessByStaffIdResponseBodyResult> getFormList() throws Exception {
        GetManageProcessByStaffIdRequest getManageProcessByStaffIdRequest = new GetManageProcessByStaffIdRequest();
        GetManageProcessByStaffIdResponse manageProcessByStaffIdWithOptions = workClient
                .getManageProcessByStaffIdWithOptions(getManageProcessByStaffIdRequest.setUserId("265502034527563439")
                        , new GetManageProcessByStaffIdHeaders().setXAcsDingtalkAccessToken(accessToken)
                        ,new RuntimeOptions());
        return manageProcessByStaffIdWithOptions.getBody().getResult();
    }
}
