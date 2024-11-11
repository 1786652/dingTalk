package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
@TableName("signet")
@Data
public class Signet {
    //用章部门
    private String section;
    //经办人
    private String agent;
    //日期
    private String date;
    //材料文件名称
    private String materialFileName;
    //文件份数
    private String documentNum;
    //文件类别
    private String documentType;
    //用章名称
    private String sealName;
    //用章类型
    private String sealType;
    //党章名称
    private String churchName;
    //备注
    private String remark;
    //附件
    private String attachments;
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
}
