package com.cyf.xiaoxuedi.service.model;

import com.cyf.xiaoxuedi.DO.MissionDO;
import com.cyf.xiaoxuedi.DO.UserDO;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderModel {

    private String id;


    private Integer userId;


    private UserDO userDO;


    private Integer accepterId;


    private UserDO accepterDO;


    private Integer missionId;


    private MissionDO missionDO;


    private BigDecimal orderPrice;


    private Byte status;
}
