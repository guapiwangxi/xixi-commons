package com.xi.commons.util;

import com.alibaba.global.satellite.proxy.message.metaq.SatelliteMessageListenerConcurrently;
import com.alibaba.global.satellite.proxy.message.metaq.SatelliteMetaPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.message.MessageExt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public abstract class BaseMetaConsumer extends SatelliteMessageListenerConcurrently implements DisposableBean, ApplicationRunner {

    private final List<SatelliteMetaPushConsumer> consumers = new ArrayList<>();

    public static final String COMMON_CONSUMER_GROUP = "CID-LAZADA-DISNEY-S";

    /**
     * 子类实现，在里面调用 [startConsumer] 方法监听 topic，可同时监听多个
     */
    protected abstract void init() throws Exception;

    /**
     * 子类实现，收到单条消息的处理逻辑
     */
    protected abstract Boolean doConsume(MessageExt msg, ConsumeConcurrentlyContext context);

    /**
     * 监听某个主题
     */
    protected void startConsumer(String topic) throws MQClientException {
        addConsumer(COMMON_CONSUMER_GROUP, topic, "*");
    }

    /**
     * 监听某个主题，带tag
     */
    protected void startConsumer(String topic, String tags) throws MQClientException {
        addConsumer(COMMON_CONSUMER_GROUP, topic, tags);
    }

    /**
     * 监听某个主题，自定义消费组名称
     */
    protected void startConsumer(String consumerGroup, String topic, String tags) throws MQClientException {
        addConsumer(consumerGroup, topic, tags);
    }

    private void addConsumer(String consumerGroup, String topic, String tags) throws MQClientException {
        SatelliteMetaPushConsumer consumer = new SatelliteMetaPushConsumer(consumerGroup);
        consumer.setConsumerGroup(consumerGroup);
        consumer.setInstanceName(consumerGroup + "-" + this.hashCode());
        consumer.setConsumeMessageBatchMaxSize(1);
        consumer.setConsumeThreadMax(5);
        consumer.subscribe(topic, tags);
        consumer.registerMessageListener(this);
        consumer.start();
        consumers.add(consumer);
    }

    /**
     * 批量消费 MetaQ 消息，循环调用子类的 [consume] 方法，只要有一个失败，则返回失败
     */
    @Override
    public ConsumeConcurrentlyStatus consume(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            try {
                boolean success = doConsume(msg, context);
                if (!success) {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            } catch (Exception e) {
                log.error("Error consuming message: ", e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
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
        for (SatelliteMetaPushConsumer consumer : consumers) {
            try {
                consumer.shutdown();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

}
