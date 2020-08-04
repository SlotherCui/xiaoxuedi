package com.cyf.xiaoxuedi.mq;

import com.alibaba.fastjson.JSON;
import com.cyf.xiaoxuedi.DAO.MissionDOMapper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Component
public class MqConsumer {
    @Value("${mq.nameserver.addr}")
    private String host;

    @Value("${mq.topic}")
    private String topic;

    private DefaultMQPushConsumer consumer;


    @Autowired
    MissionDOMapper missionDOMapper;

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer("status_group");
        consumer.setNamesrvAddr(host);
        consumer.subscribe(topic,"*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                Message msg = list.get(0);
                System.out.println(msg);
                try{
                    Map<String , Object>  bodyMap =JSON.parseObject(new String(msg.getBody()),Map.class);
                    Integer missionId = (Integer) bodyMap.get("missionId");
                    Byte status = ((Integer) bodyMap.get("status")).byteValue();
                        missionDOMapper.updateStatusByPrimaryKey(status,missionId);

                }catch (Exception e){
                    e.printStackTrace();
                    throw  e;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
    }



}
