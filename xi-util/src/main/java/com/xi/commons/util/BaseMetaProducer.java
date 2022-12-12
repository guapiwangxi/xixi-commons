package com.xi.commons.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.global.satellite.proxy.message.metaq.SatelliteMetaProducer;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.client.producer.SendStatus;
import com.alibaba.rocketmq.common.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;


@Slf4j
public abstract class BaseMetaProducer implements DisposableBean, ApplicationRunner {

    private SatelliteMetaProducer producer;

    protected String PRODUCER_GROUP_PREFIX = "LAZADA-DISNEY-S-";

    /**
     * 子类实现
     */
    protected abstract void init() throws Exception;

    /**
     * 子类调用
     */
    protected String doSendMsg(String topic, String tag, String key, Object message) {
        return sendMessage(producer, topic, tag, key, message);
    }

    protected String doSendMsg(String topic, String key, Object message) {
        return sendMessage(producer, topic, "*", key, message);
    }

    protected void startProducer(String producerGroup) throws MQClientException {
        addProducer(PRODUCER_GROUP_PREFIX + producerGroup);
    }

    private void addProducer(String producerGroup) throws MQClientException {
        SatelliteMetaProducer metaProducer = new SatelliteMetaProducer();
        metaProducer.setProducerGroup(producerGroup);
        metaProducer.setInstanceName(producerGroup + "-" + this.hashCode());
        metaProducer.start();
        producer = metaProducer;
    }

    public String sendMessage(SatelliteMetaProducer producer, String topic, String tag, String key, Object message) {
        String messageBody = JSON.toJSONString(message);
        try {
            Message msg = new Message(topic, tag, key, (messageBody).getBytes());
            SendResult result = producer.send(msg);
            if (result.getSendStatus() == SendStatus.SEND_OK) {
                log.info("Send message successfully, msgId:{}", result.getMsgId());
                return result.getMsgId();
            } else {
                log.error("Send message fail. msg:{}, response:{}", messageBody, JSON.toJSONString(result));
            }
        } catch (Exception e) {
            log.info("Error occurs when send message: {}.\n{}", messageBody, ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

    /**
     * 自动初始化
     * 此处不使用 [PostConstruct] 注解，因为会可能出现消费消息时 Spring 容器还没有完全初始化的情况，导致应用重启时出现大量异常
     * [ApplicationRunner] 接口可以保证在 Spring 容器初始化完成之后再调用
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        init();
    }

    /**
     * 自动销毁
     */
    @Override
    public void destroy() {
        this.producer.shutdown();
    }

}
