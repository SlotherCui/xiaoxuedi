package com.cyf.xiaoxuedi.service.impl;

import com.cyf.xiaoxuedi.DAO.MissionDOMapper;
import com.cyf.xiaoxuedi.DAO.OrderDOMapper;
import com.cyf.xiaoxuedi.DAO.SequenceDOMapper;
import com.cyf.xiaoxuedi.DAO.UserDOMapper;
import com.cyf.xiaoxuedi.DO.MissionDO;
import com.cyf.xiaoxuedi.DO.OrderDO;
import com.cyf.xiaoxuedi.DO.SequenceDO;
import com.cyf.xiaoxuedi.DO.UserDO;
import com.cyf.xiaoxuedi.error.BuinessException;
import com.cyf.xiaoxuedi.error.EmBusinessError;
import com.cyf.xiaoxuedi.service.OrderService;
import com.cyf.xiaoxuedi.service.model.MissionItemModel;
import com.cyf.xiaoxuedi.service.model.OrderModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    MissionDOMapper missionDOMapper;

    @Autowired
    UserDOMapper userDOMapper;

    @Autowired
    SequenceDOMapper sequenceDOMapper;

    @Autowired
    OrderDOMapper orderDOMapper;

    @Autowired
    RedisTemplate redisTemplate;

    @Override
    @Transactional
    public OrderModel acceptMission(Integer missionId, Integer userId) throws BuinessException {
        //1. 查询Mission_id 验证是否存在
        MissionDO missionDO = missionDOMapper.selectByPrimaryKey(missionId);
        if(missionDO==null){
            throw new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }


        //2. 查询User_id 验证是否存在
        UserDO publisher =userDOMapper.selectByPrimaryKey(missionDO.getUserId());
        if(publisher==null){
            throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
        }

        UserDO accepter = userDOMapper.selectByPrimaryKey(userId);
        if(accepter==null){
            throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
        }

        // 2.5 不能抢自己的任务
        if(publisher.getId()==accepter.getId()){
            throw new BuinessException(EmBusinessError.MISSION_BY_YOURSELF);
        }


        // 3. 验证任务是否被抢
        if(missionDO.getStatus()!=0){
            throw new BuinessException(EmBusinessError.MISSION_HAS_GONE);
        }


        //4. 订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setMissionId(missionId);
        orderModel.setMissionDO(missionDO);
        orderModel.setOrderPrice(missionDO.getPrice());
        orderModel.setUserId(publisher.getId());
        orderModel.setUserDO(publisher);
        orderModel.setAccepterId(accepter.getId());
        orderModel.setAccepterDO(accepter);
        orderModel.setStatus((byte) 0);

        // 生产订单流水号
        String orderId = generatorOrderId();
        orderModel.setId(orderId);

        // 入库
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        orderDOMapper.insertSelective(orderDO);

        //5. 改变商品状态
        missionDO.setStatus((byte) 1);
        missionDOMapper.updateByPrimaryKey(missionDO);

        //6. 返回前端

        return orderModel;
    }

    @Override
    @Transactional
    public void finishOrder(String orderId, Integer userId) throws BuinessException {
        //1. 查询order_id 验证是否存在
        OrderDO orderDO = orderDOMapper.selectByPrimaryKey(orderId);
        if(orderDO==null){
            throw new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }


        MissionDO missionDO = missionDOMapper.selectByPrimaryKey(orderDO.getMissionId());
        if(missionDO==null){
            throw new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        // 2. 更改订单状态
        orderDO.setStatus((byte) 1);
        orderDOMapper.updateByPrimaryKeySelective(orderDO);

        // 3.更改Mission的状态
        missionDO.setStatus((byte) 2);
        missionDOMapper.updateByPrimaryKeySelective(missionDO);

    }

    @Override
    public List<OrderModel> getCurrentOrderList(Integer userId, Integer type, Integer page) throws BuinessException {

        // 获得分页偏移量
        final int pageSize = 10;
        Integer offset = page * pageSize;


        // 验证入参
        if(page<0){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"页号为负数");
        }

        // 分类型查询订单
        List<OrderDO> list = null;
        if(type==0){
            list = orderDOMapper.selectByUserId(userId, (byte) 0,offset);
        }else if(type==1){
            list = orderDOMapper.selectByAccepterId(userId, (byte) 0,offset);
        }else{
            throw new BuinessException(EmBusinessError.UNKNOWN_ERROR,"检索状态异常");
        }

        List<OrderModel> orderModels = list.stream().map(orderDO -> {
            OrderModel orderModel = new OrderModel();
            BeanUtils.copyProperties(orderDO,orderModel);

            // 获得发布人, 接收人姓名 并将UserDO缓存到Redis中
            UserDO userDO = (UserDO) redisTemplate.opsForValue().get("User_"+orderDO.getUserId());
            if(userDO==null){
                userDO = userDOMapper.selectByPrimaryKey(orderDO.getUserId());
                redisTemplate.opsForValue().set("User_"+orderDO.getUserId(), userDO);
                redisTemplate.expire("User_"+orderDO.getUserId(), 10, TimeUnit.MINUTES);
            }
            orderModel.setUserDO(userDO);

            UserDO accepterDO = (UserDO) redisTemplate.opsForValue().get("User_"+orderDO.getAccepterId());
            if(accepterDO==null){
                accepterDO = userDOMapper.selectByPrimaryKey(orderDO.getAccepterId());
                redisTemplate.opsForValue().set("User_"+orderDO.getAccepterId(), accepterDO);
                redisTemplate.expire("User_"+orderDO.getAccepterId(), 10, TimeUnit.MINUTES);
            }
            orderModel.setAccepterDO(accepterDO);

            // 获得任务
            MissionDO missionDO = missionDOMapper.selectByPrimaryKey(orderDO.getMissionId());
            orderModel.setMissionDO(missionDO);

            return orderModel;
        }).collect(Collectors.toList());

        return orderModels;
    }


    /**
     * 生成订单流水号
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generatorOrderId(){
        StringBuilder stringBuilder = new StringBuilder();
        //订单号有16位 前8位为时间信息，年月日，中间6位为自增序列，最后2位为分库分表位暂时写死
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);
        //获取当前sequence
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.selectByPrimaryKey("order_info");
        sequence = sequenceDO.getSequence();
        sequenceDO.setSequence(sequenceDO.getSequence() + sequenceDO.getStep());

        // 判定是否超出限制
        if(sequenceDO.getSequence()>sequenceDO.getMaxNumber()){
            sequenceDO.setSequence(sequenceDO.getInitNumber());
        }
        sequenceDOMapper.updateByPrimaryKey(sequenceDO);

        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i ++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        // 分库分表位
        stringBuilder.append("00");
        return stringBuilder.toString();
    }


}
