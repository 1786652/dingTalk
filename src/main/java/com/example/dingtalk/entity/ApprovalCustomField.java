package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("approval_custom_field")
public class ApprovalCustomField {
    @TableId(type = IdType.INPUT)
    private String id;
    private String processInstanceId;
    private String fieldName;
    private String fieldValue;
}
