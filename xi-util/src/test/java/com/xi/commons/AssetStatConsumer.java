package com.xi.commons;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.taobao.common.fulllinkstresstesting.StressTestingUtil;
import com.xi.commons.aspect.advice.UseZLog;
import com.xi.commons.logger.ZLog;
import com.xi.commons.util.BaseMetaConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AssetStatConsumer extends BaseMetaConsumer {

    @Override
    protected void init() throws Exception {
        startConsumer("DISNEY-ASSET-METRIC", "LAZ-DISNEY-ASSET-CHANGED", "*");
    }

    @Override
    @UseZLog(logger = "ASSET")
    protected Boolean doConsume(MessageExt msg, ConsumeConcurrentlyContext context) {
        if (StressTestingUtil.isTestFlow()) {
            return true;
        }

        String json = new String(msg.getBody(), StandardCharsets.UTF_8);

        try {
            CacheConfig dto = JSON.parseObject(json, CacheConfig.class);
            ZLog.ap("event", dto)
                .ap("consume", "success");

            return true;
        } catch (Exception e) {
            ZLog.ap("consume", "failed");
            log.error("consumer msg failed, msg={}, e=", json, e);
            return false;
        }
    }


}
