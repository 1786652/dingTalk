package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("AllTask")
public class AllTask {
    @TableId(value = "businessId")
    private String businessId;
    @TableField(value = "processCode")
    private String processCode;
    @TableField(value = "title")
    private String title;
    @TableField(value = "createTime")
    private String createTime;
    @TableField(value = "finishTime")
    private String finishTime;
    @TableField(value = "users")
    private String users;
}
