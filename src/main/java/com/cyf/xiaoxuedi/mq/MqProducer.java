package com.cyf.xiaoxuedi.mq;

import com.alibaba.fastjson.JSON;
import com.cyf.xiaoxuedi.DO.MissionDO;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Value;
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

    @PostConstruct
    public void init() throws MQClientException {
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(host);
        producer.start();
    }

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
