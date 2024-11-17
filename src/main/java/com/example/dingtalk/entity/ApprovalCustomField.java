package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("approval_custom_field")
public class ApprovalCustomField {
    @TableId(type = IdType.AUTO)
    private String id;
    private String processInstanceId;
    private String processCode;
    private String businessId;
    private String fieldName;
    private String fieldValue;
}
