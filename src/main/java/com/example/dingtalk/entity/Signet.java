package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
@TableName("signet")
@Data
public class Signet {
    //表单ID
    private String processCode;
    @TableId(value="business_id",type = IdType.INPUT)
    //编号
    private String businessId;
    //审批单名
    private String title;
    //开始时间
    private Date createTime;
    //结束时间
    private Date finishTime;
    //审批人
    private String taskUsername;
    //实例id
    private String processInstanceId;
    //流程
    private String flow;
    //抄送人
    private String ccUserIds;
    // 发起人
    private String originatorUserId;
    // 部门名
    private String depName;
}
