package com.example.dingtalk.config;

import com.example.dingtalk.job.RefreshJob2;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail myJobDetail() {
        return JobBuilder.newJob(RefreshJob2.class)
                .withIdentity("myJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger myTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(myJobDetail())
                .withIdentity("myTrigger")
//                .withSchedule(CronScheduleBuilder.cronSchedule("0 */10 * * * ?")) // 每10秒执行一次
                .build();
//        return null;
    }
}
