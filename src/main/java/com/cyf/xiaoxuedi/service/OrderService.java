package com.cyf.xiaoxuedi.service;

import com.cyf.xiaoxuedi.error.BusinessException;
import com.cyf.xiaoxuedi.service.model.OrderModel;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderService {
    OrderModel acceptMission(Integer missionId, Integer userId) throws BusinessException;
    OrderModel acceptMissionFaster(Integer missionId, Integer userId) throws BusinessException;

    @Transactional(rollbackFor = BusinessException.class)
    OrderModel acceptMissionFasterTransactional(Integer missionId, Integer userId) throws BusinessException;

    void finishOrder(String orderId, Integer userId) throws BusinessException;
    List<OrderModel> getCurrentOrderList(Integer userId, Integer type , Integer page) throws BusinessException;
}
