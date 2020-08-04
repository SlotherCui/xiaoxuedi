package com.cyf.xiaoxuedi.mq;

import com.alibaba.fastjson.JSON;
import com.cyf.xiaoxuedi.DAO.OrderDOMapper;
import com.cyf.xiaoxuedi.DO.MissionDO;
import com.cyf.xiaoxuedi.DO.OrderDO;
import com.cyf.xiaoxuedi.error.BusinessException;
import com.cyf.xiaoxuedi.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

// mqadmin updateTopic -n localhost:9876 -t status -c DefaultCluster
@Component
public class MqProducer {

    @Value("${mq.nameserver.addr}")
    private String host;

    @Value("${mq.topic}")
    private String topic;

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Autowired
    OrderService orderService;

    @Autowired
    OrderDOMapper orderDOMapper;

    @Autowired
    RedisTemplate redisTemplate;

    @PostConstruct
    public void init() throws MQClientException {
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(host);
        producer.start();


        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(host);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object o) {
                Integer missionId = (Integer) ((Map)o).get("missionId");
                Integer userId = (Integer) ((Map)o).get("userId");

                try {
                    orderService.acceptMissionFasterTransactional(missionId,userId);
                } catch (Exception e) {
                    e.printStackTrace();

                    //回滚缓存记录
                    MissionDO missionDO = (MissionDO) redisTemplate.opsForValue().get("Mission_"+missionId);
                    missionDO.setStatus((byte) 0);
                    redisTemplate.opsForValue().set("Mission_"+missionId, missionDO);

                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {

                String json = new String(messageExt.getBody());
                Map<String , Object>map = JSON.parseObject(json,Map.class);
                Integer missionId = (Integer) map.get("missionId");
                Integer userId = (Integer)map.get("userId");


                // 查询订单表验证消息状态，如果存在则提交，不存在则回滚。
                OrderDO orderDO = orderDOMapper.selectByMissionId(missionId);
                if(orderDO==null){
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }else{
                    return LocalTransactionState.COMMIT_MESSAGE;
                }

            }
        });

    }

    /**
     * 事务性异步更改任务状态消息
     * @param missionId
     * @param status
     * @return
     */
    public boolean TransactionSendChangeMissionStatus(Integer missionId,Byte status,Integer userId)  {

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("missionId",missionId);
        bodyMap.put("status",status);
        Message message = new Message(topic,"ChangeMissionStatus", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("missionId",missionId);
        argsMap.put("userId",userId);

        TransactionSendResult sendResult = null;
        try {
            sendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);

        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }

        if(sendResult.getLocalTransactionState()==LocalTransactionState.ROLLBACK_MESSAGE){
            return false;
        }else if(sendResult.getLocalTransactionState()==LocalTransactionState.COMMIT_MESSAGE){
            return true;
        }else{
            return false;
        }

    }

    /**
     * 异步更改任务状态消息
     * @param missionId
     * @param status
     * @return
     */
    public boolean SendChangeMissionStatus(Integer missionId,Byte status) {

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("missionId",missionId);
        bodyMap.put("status",status);
        Message message = new Message(topic,"ChangeMissionStatus", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

        try {
           producer.send(message);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }



}
