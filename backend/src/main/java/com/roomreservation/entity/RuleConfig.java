package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 规则参数表，键值存储，管理端可调
 */
@Data
@TableName("rule_config")
public class RuleConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 规则键 */
    private String ruleKey;

    /** 规则值 */
    private String ruleValue;

    /** 说明 */
    private String note;
}
