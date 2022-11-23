package com.xi.commons.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.taobao.hsf.context.RPCContext;
import com.taobao.mtop.api.agent.MtopContext;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 获取请求上下文信息的工具类，兼容袋鼠预加载请求和 mtop 请求
 *
 * 如果前端使用了袋鼠预加载进行页面首屏优化，流量会经过袋鼠再到我们的业务服务器，这会导致 mtop 的上下文丢失。
 * 袋鼠使用 HSF 请求我们的接口，这时 mtop 上下文应该从 RPCContext 中获取
 *
 * 请求链路：前端 --> mtop --> 袋鼠 --> 业务服务端
 *
 * 具体文档参见：https://yuque.antfin.com/kkgt0c/pekrad/erqs33#SKLBT
 */
public class RequestUtils {

    /**
     * 判断当前流量是否来自袋鼠
     */
    public static boolean isRequestFromKangaroo() {
        RPCContext rpcContext = RPCContext.getServerContext();
        if (rpcContext != null) {
            return MapUtils.getBooleanValue(rpcContext.getAttachments(), "fromKangaroo");
        } else {
            return false;
        }
    }

    /**
     * 判断当前流量是否来自 mtop
     */
    public static boolean isRequestFromMtop() {
        return MtopContext.isRequestFromMtop();
    }

    /**
     * 获取当前登录用户 ID
     */
    public static long getUserId() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getLongValue(RPCContext.getServerContext().getAttachments(), "userNumbId");
        } else {
            return MtopContext.getUserId();
        }
    }

    /**
     * 获取当前请求的 user agent
     */
    public static String getUserAgent() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "uaMtopUAString");
        } else {
            return MtopContext.getUserAgent();
        }
    }

    /**
     * 获取当前请求的 app key
     */
    public static String getAppKey() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "appKey");
        } else {
            return MtopContext.getAppKey();
        }
    }

    /**
     * 获取当前请求的 app version
     */
    public static String getAppVersion() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "appVersion");
        } else {
            return MtopContext.getAppVersion();
        }
    }

    /**
     * 获取当前用户的平台
     */
    public static String getPlatform() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "platform");
        } else {
            return MtopContext.getPlatform();
        }
    }

    /**
     * 获取当前用户使用的语言
     */
    public static String getLanguage() {
        String language;
        if (isRequestFromKangaroo()) {
            language = StringUtils.lowerCase(MapUtils.getString(RPCContext.getServerContext().getAttachments(), "language"));
        } else {
            language = MtopContext.getXLang();
        }

        // 针对 en-MY 这样的格式进行处理，只返回语言部分，即 en
        if (language != null && language.contains("-")) {
            String[] arr = language.split("-");
            if (arr.length > 0) {
                language = arr[0];
            }
        }

        return language;
    }

    /**
     * 获取当前用户所在的国家
     */
    public static String getRegionId() {
        if (isRequestFromKangaroo()) {
            return StringUtils.upperCase(MapUtils.getString(RPCContext.getServerContext().getAttachments(), "regionId"));
        } else {
            return MtopContext.getXRegion();
        }
    }

    /**
     * 获取当前请求的 IP 地址
     */
    public static String getIp() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "ip");
        } else {
            return MtopContext.getIP();
        }
    }

    /**
     * 获取当前请求的 utdid
     */
    public static String getUtdid() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "utdid");
        } else {
            return MtopContext.getUtdid();
        }
    }

    /**
     * 获取当前请求的 umid
     */
    public static String getUmid() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "umid");
        } else {
            return MtopContext.getUmid();
        }
    }

    /**
     * 获取当前请求的 umid token
     */
    public static String getUmidToken() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "headerXUmidToken");
        } else {
            return MtopContext.getHeader("x-umidtoken");
        }
    }

    /**
     * 获取当前请求的 cna
     */
    public static String getCna() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "cookieCna");
        } else {
            return MtopContext.getCookie("cna");
        }
    }

    /**
     * 获取当前请求的 x-server-env-raw
     */
    public static String getServerEnvRaw() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "headerXServerEnvRaw");
        } else {
            return MtopContext.getHeader("x-server-env-raw");
        }
    }

    /**
     * 获取当前请求的 wua
     */
    public static String getWua() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "wua");
        } else {
            return MtopContext.getWua();
        }
    }

    /**
     * 获取当前请求的 mini wua
     */
    public static String getMiniWua() {
        if (isRequestFromKangaroo()) {
            return MapUtils.getString(RPCContext.getServerContext().getAttachments(), "headerXMiniWua");
        } else {
            return MtopContext.getHeader("x-mini-wua");
        }
    }

    /**
     * 获取当前页面的 url
     */
    public static String getPageUrl() {
        String url;
        if (isRequestFromKangaroo()) {
            url = MapUtils.getString(RPCContext.getServerContext().getAttachments(), "url");
        } else {
            url = MtopContext.getHeader("referer");
        }

        if (url != null) {
            if (url.startsWith("https%3A%2F%2F") || url.startsWith("http%3A%2F%2F")) {
                try {
                    url = URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new AssertionError(e);
                }
            }
        }

        return url;
    }
}
