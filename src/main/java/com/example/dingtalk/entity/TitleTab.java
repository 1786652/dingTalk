package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("TitleTab")
public class TitleTab {
    private Integer attendanceType;
    private String flowTitle;
    private String gmtModified;
    private String iconName;
    private String iconUrl;
    private Boolean newProcess;
    @TableId(value = "process_code")
    private String processCode;
}
