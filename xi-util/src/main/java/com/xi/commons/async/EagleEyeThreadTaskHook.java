package com.xi.commons.async;

import com.alibaba.global.satellite.annotation.SatelliteHook;
import com.alibaba.global.satellite.hook.concurrent.ThreadTaskHook;

import com.taobao.eagleeye.EagleEye;
import com.taobao.eagleeye.RpcContext_inner;

/**
 * 跨线程传递鹰眼上下文
 */
@SatelliteHook
public class EagleEyeThreadTaskHook implements ThreadTaskHook {

    @Override
    public Object getObjFromInit() {
        return EagleEye.getRpcContext();
    }

    @Override
    public Object doBefore(Object objFromInit) {
        RpcContext_inner originContext = EagleEye.getRpcContext();
        EagleEye.setRpcContext((RpcContext_inner) objFromInit);
        return originContext;
    }

    @Override
    public void doAfter(Object objFromInit, Object objFromDoBefore) {
        EagleEye.setRpcContext((RpcContext_inner) objFromDoBefore);
    }
}
