package com.cyf.xiaoxuedi.service;

import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.service.model.OrderModel;

import java.util.List;

public interface OrderService {
    OrderModel acceptMission(Integer missionId, Integer userId) throws BuinessException;
    void finishOrder(String orderId, Integer userId) throws BuinessException;
    List<OrderModel> getCurrentOrderList(Integer userId, Integer type , Integer page) throws BuinessException;
}
