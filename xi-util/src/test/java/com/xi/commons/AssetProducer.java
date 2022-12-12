package com.xi.commons;

import com.xi.commons.aspect.advice.UseZLog;
import com.xi.commons.util.BaseMetaProducer;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Component;


@Component
public class AssetProducer extends BaseMetaProducer {

    private static final String PRODUCER_GROUP = "DISNEY_ASSET";

    @Override
    protected void init() throws Exception {
        startProducer(PRODUCER_GROUP);
    }

    @UseZLog(logger = "ASSET")
    public void sendNewRecordEvent(CacheConfig record) {
        doSendMsg("TOPIC_DISNEY_ASSET_CHANGED_EVENT", "tag", "key", record);
    }

}
