package com.cyf.xiaoxuedi.service.model;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class MissionItemModel {
    private Integer id;

    private Integer userId;

    private String userName;

    private String title;

    private Byte status;

    private BigDecimal price;

    private Date createTime;
}
