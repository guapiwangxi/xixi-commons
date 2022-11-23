package com.xi.commons.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;

import com.alibaba.ecommerce.international.ILocalizationService;
import com.alibaba.ecommerce.international.model.LocalizationInfo;
import com.alibaba.global.g11n.admin.G11nConfigHolder;
import com.alibaba.global.g11n.admin.G11nRepoHolder;
import com.alibaba.global.g11n.model.CurrencySpec;
import com.alibaba.global.g11n.model.DisplayStyle;
import com.alibaba.global.g11n.utils.MonetaryUtils;

import com.taobao.hsf.annotation.Order;
import com.taobao.hsf.context.RPCContext;
import com.taobao.hsf.invocation.Invocation;
import com.taobao.hsf.invocation.InvocationHandler;
import com.taobao.hsf.invocation.RPCResult;
import com.taobao.hsf.invocation.filter.ClientFilter;
import com.taobao.hsf.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.StringUtils;
import org.javamoney.moneta.spi.MoneyUtils;

/**
 * Created by vince at Apr 25, 2022.
 */
public class LocalizationUtils {

    /**
     * 获取当前用户的 Locale
     */
    public static Locale getLocale() {
        if (RequestUtils.isRequestFromKangaroo()) {
            // 针对来自袋鼠的流量，直接从请求上下文中取出 locale 信息
            return new Locale(RequestUtils.getLanguage(), RequestUtils.getRegionId());
        } else {
            // 针对 mtop 或其他来源的流量，走 es-boot 框架的逻辑
            return SpringUtils.getBean(ILocalizationService.class).getCurrentLocalizationInfo().getLocale();
        }
    }

    /**
     * 获取当前用户的本地化配置
     */
    public static LocalizationInfo getLocalizationInfo() {
        if (RequestUtils.isRequestFromKangaroo()) {
            // 针对来自袋鼠的流量，直接从请求上下文中取出 locale 信息，然后获取对应的本地化配置
            Locale locale = new Locale(RequestUtils.getLanguage(), RequestUtils.getRegionId());
            return SpringUtils.getBean(ILocalizationService.class).getLocalizationInfoByLocale(locale);
        } else {
            // 针对 mtop 或其他来源的流量，走 es-boot 框架的逻辑
            return SpringUtils.getBean(ILocalizationService.class).getCurrentLocalizationInfo();
        }
    }

    /**
     * 获取当前的时区
     */
    public static ZoneId getZoneId() {
        return getLocalizationInfo().getZoneId();
    }

    /**
     * 获取当前的货币符号
     */
    public static String getCurrencySymbol() {
        return getLocalizationInfo().getCurrencySymbol();
    }

    /**
     * 获取当前的货币代码
     */
    public static String getCurrencyCode() {
        return getLocalizationInfo().getCurrencyUnit().getCurrencyCode();
    }

    /**
     * 获取当前的货币单位
     */
    public static CurrencyUnit getCurrencyUnit() {
        return getLocalizationInfo().getCurrencyUnit();
    }

    /**
     * 判断当前的货币符号在格式化时是否放在数字左边
     */
    public static boolean isCurrencyLeft() {
        LocalizationInfo info = getLocalizationInfo();
        CurrencySpec currencySpec = G11nConfigHolder.getCurrencySpec(info.getCurrencyUnit().getCurrencyCode(), DisplayStyle.LOCAL);
        return currencySpec.getFormat(info.getLocale().getLanguage()).isShowSymbolInLeft();
    }

    /**
     * 获取 MonetaryAmount 对象
     *
     * 注意：
     * 1. 如果传入的数字类型为 long，使用最小货币单位，其他数字类型使用正常单位（元）
     * 2. 最小货币单位一般是分，但越南是元
     */
    public static MonetaryAmount getMonetaryAmount(Number amount) {
        LocalizationInfo info = getLocalizationInfo();

        if (amount instanceof Long) {
            return MonetaryUtils.of(amount.longValue(), info.getCurrencyUnit());
        } else {
            return MonetaryUtils.of(MoneyUtils.getBigDecimal(amount), info.getCurrencyUnit());
        }
    }

    /**
     * 货币格式化
     *
     * 注意：
     * 1. 如果传入的数字类型为 long，使用最小货币单位，其他数字类型使用正常单位（元）
     * 2. 最小货币单位一般是分，但越南是元
     */
    public static String formatMoney(Number amount) {
        LocalizationInfo info = getLocalizationInfo();

        MonetaryAmount monetaryAmount;
        if (amount instanceof Long) {
            monetaryAmount = MonetaryUtils.of(amount.longValue(), info.getCurrencyUnit());
        } else {
            monetaryAmount = MonetaryUtils.of(MoneyUtils.getBigDecimal(amount), info.getCurrencyUnit());
        }

        CurrencySpec currencySpec = G11nConfigHolder.getCurrencySpec(info.getCurrencyUnit().getCurrencyCode(), DisplayStyle.LOCAL);
        return currencySpec.getFormat(info.getLocale().getLanguage()).format(monetaryAmount);
    }

    /**
     * 货币格式化（不带货币符号）
     *
     * 注意：
     * 1. 如果传入的数字类型为 long，使用最小货币单位，其他数字类型使用正常单位（元）
     * 2. 最小货币单位一般是分，但越南是元
     */
    public static String formatMoneyWithoutSymbol(Number amount) {
        LocalizationInfo info = getLocalizationInfo();

        MonetaryAmount monetaryAmount;
        if (amount instanceof Long) {
            monetaryAmount = MonetaryUtils.of(amount.longValue(), info.getCurrencyUnit());
        } else {
            monetaryAmount = MonetaryUtils.of(MoneyUtils.getBigDecimal(amount), info.getCurrencyUnit());
        }

        CurrencySpec currencySpec = G11nConfigHolder.getCurrencySpec(info.getCurrencyUnit().getCurrencyCode(), DisplayStyle.LOCAL);
        return currencySpec.getFormat(info.getLocale().getLanguage()).noSymbolFormat(monetaryAmount);
    }

    /**
     * 获取多语言文案
     */
    public static String getText(String key) {
        Locale locale = getLocale();
        String text = G11nRepoHolder.getTextResourceRepo().get(key, locale);
        return StringUtils.defaultIfBlank(text, key);
    }

    /**
     * 获取多语言文案
     */
    public static String getText(String key, Locale locale) {
        String text = G11nRepoHolder.getTextResourceRepo().get(key, locale);
        return StringUtils.defaultIfBlank(text, key);
    }

    /**
     * 获取多语言文案
     */
    public static String getTextOrNull(String key) {
        Locale locale = getLocale();
        return G11nRepoHolder.getTextResourceRepo().get(key, locale);
    }

    /**
     * 获取多语言文案
     */
    public static String getTextOrNull(String key, Locale locale) {
        return G11nRepoHolder.getTextResourceRepo().get(key, locale);
    }

    /**
     * 获取当前时间（带时区）
     */
    public static ZonedDateTime getZonedDateTime() {
        LocalizationInfo info = getLocalizationInfo();
        return ZonedDateTime.now(info.getZoneId());
    }

    /**
     * 获取当前时间（带时区）
     */
    public static ZonedDateTime getZonedDateTime(long millis) {
        LocalizationInfo info = getLocalizationInfo();
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), info.getZoneId());
    }

    /**
     * 获取当前日期（当地）
     */
    public static LocalDate getLocalDate() {
        LocalizationInfo info = getLocalizationInfo();
        return LocalDate.now(info.getZoneId());
    }

    /**
     * 获取当前日期（当地）
     */
    public static LocalDate getLocalDate(long millis) {
        LocalizationInfo info = getLocalizationInfo();
        return Instant.ofEpochMilli(millis).atZone(info.getZoneId()).toLocalDate();
    }

    /**
     * 把来自袋鼠的 Locale 信息透传到下游，避免下游无法获取本地化信息的问题，参考 es-boot 框架的默认逻辑
     *
     * @see com.alibaba.ecommerce.international.filter.HsfConsumerFilterImpl
     */
    @Order(0) // 保证在 es-boot 框架之前执行
    public static class LocalizationHsfClientFilter implements ClientFilter {

        @Override
        public ListenableFuture<RPCResult> invoke(InvocationHandler nextHandler, Invocation invocation) throws Throwable {
            RPCContext context = RPCContext.getClientContext();

            if (context != null && RequestUtils.isRequestFromKangaroo()) {
                ILocalizationService service = SpringUtils.getBean(ILocalizationService.class);
                String paramName = service.getProperties().getHsfContextParamNameForKey();

                if (context.getAttachment(paramName) == null) {
                    Locale locale = new Locale(RequestUtils.getLanguage(), RequestUtils.getRegionId());
                    LocalizationInfo info = service.getLocalizationInfoByLocale(locale);

                    if (info != null) {
                        context.putAttachment(paramName, info.getKey());
                    }
                }
            }

            return nextHandler.invoke(invocation);
        }

        @Override
        public void onResponse(Invocation invocation, RPCResult rpcResult) {
            // do nothing...
        }
    }
}
