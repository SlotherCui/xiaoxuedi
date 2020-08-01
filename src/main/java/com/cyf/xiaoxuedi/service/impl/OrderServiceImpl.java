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
import com.cyf.xiaoxuedi.service.MissionService;
import com.cyf.xiaoxuedi.service.OrderService;
import com.cyf.xiaoxuedi.service.UserService;
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

    @Autowired
    UserService userService;

    @Autowired
    MissionService missionService;

    /**
     * 抢任务的较强一致性方案 // 如果事务执行失败，需要外部try catch还原Redis    // 因为缓存中判断任务是否被抢的标记的过期时间比Mission_更长
     * @param missionId
     * @param userId
     * @return
     * @throws BuinessException
     */
    @Override
    @Transactional
    public OrderModel acceptMission(Integer missionId, Integer userId) throws BuinessException {

        //1. 查询Mission_id 验证是否存在
        //MissionDO missionDO = missionDOMapper.selectByPrimaryKey(missionId);
        // 查缓存
        MissionDO missionDO = missionService.getMissionDOByIdInCache(missionId);
        if(missionDO==null){
            throw new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        // 1.5 判断该任务是否已被抢， 防止当缓存中任务被抢的标志位过期后的更改。
        if(missionDO.getStatus()!=0){
            throw new BuinessException(EmBusinessError.MISSION_HAS_GONE);
        }



        //2. 查询发起者，接收者 验证是否存在
        UserDO publisher = userService.getUserDOByIdInCache(missionDO.getUserId());
        if(publisher==null){
            throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
        }

        UserDO accepter = userService.getUserDOByIdInCache(userId);
        if(accepter==null){
            throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
        }

        // 2.5 不能抢自己的任务
        if(publisher.getId()==accepter.getId()){
            throw new BuinessException(EmBusinessError.MISSION_BY_YOURSELF);
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

        //6. 删缓存
        redisTemplate.delete("Mission_"+missionId);

        //7. 返回前端
        return orderModel;
    }

    /**
     * 抢任务的较保证响应速度的实现方式，采用异步更新Mission状态
     * @param missionId
     * @param userId
     * @return
     * @throws BuinessException
     */
    @Override
    public OrderModel acceptMissionFaster(Integer missionId, Integer userId) throws BuinessException {
        return null;
    }


    @Override
    @Transactional
    public void finishOrder(String orderId, Integer userId) throws BuinessException {
        //1. 查询order_id 验证是否存在
        OrderDO orderDO = orderDOMapper.selectByPrimaryKey(orderId);
        if(orderDO==null){
            throw new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        // 1.1 不能结束掉别人的任务
        if(orderDO.getUserId() != userId){
            throw new BuinessException(EmBusinessError.UNKNOWN_ERROR,"无权访问");
        }


        // 1.2 验证任务存在
        MissionDO missionDO = missionService.getMissionDOByIdInCache(orderDO.getMissionId());
        if(missionDO==null){
            throw new BuinessException(EmBusinessError.MISSION_NOT_EXIT);
        }

        // 1.3 正在进行中的任务才能结束
        if(orderDO.getStatus()!=0){
            throw new BuinessException(EmBusinessError.UNKNOWN_ERROR,"已经结束了");
        }

        // 2. 更改订单状态
        orderDO.setStatus((byte) 1);
        orderDOMapper.updateByPrimaryKeySelective(orderDO);

        // 3.更改Mission的状态
        missionDO.setStatus((byte) 2);
        missionDOMapper.updateByPrimaryKeySelective(missionDO);

        // 3.1 删除Redis中Mission
        redisTemplate.delete("Mission_"+missionDO.getId());

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
            UserDO userDO = userService.getUserDOByIdInCache(orderDO.getUserId());
            orderModel.setUserDO(userDO);

            UserDO accepterDO = userService.getUserDOByIdInCache(orderDO.getAccepterId());
            orderModel.setAccepterDO(accepterDO);

            // 获得任务 通过Cache
            MissionDO missionDO = missionService.getMissionDOByIdInCache(orderDO.getMissionId());
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
