/*
 * Copyright 2018 JessYan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.jessyan.autosize;

import java.util.*;

import me.jessyan.autosize.external.ExternalAdaptInfo;
import me.jessyan.autosize.internal.CustomAdapt;
import me.jessyan.autosize.utils.AutoSizeLog;
import me.jessyan.autosize.utils.Preconditions;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.ability.AbilityPackage;
import ohos.aafwk.ability.DataAbilityHelper;
import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.agp.window.service.Display;
import ohos.agp.window.service.DisplayAttributes;
import ohos.agp.window.service.DisplayManager;
import ohos.app.Context;
import ohos.global.configuration.Configuration;
import ohos.global.configuration.DeviceCapability;
import ohos.global.resource.ResourceManager;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import ohos.utils.net.Uri;

/**
 * ================================================
 * AndroidAutoSize 用于屏幕适配的核心方法都在这里, 核心原理来自于 <a href="https://mp.weixin.qq.com/s/d9QCoBP6kV9VSWvVldVVwA">今日头条官方适配方案</a>
 * 此方案只要应用到 {@link_TODO Activity} 上, 这个 {@link_TODO Activity} 下的所有 Fragment、{@link_TODO Dialog}、
 * 自定义 {@link_TODO View} 都会达到适配的效果, 如果某个页面不想使用适配请让该 {@link_TODO Activity} 实现 {@link_TODO CancelAdapt}
 * <p>
 * 任何方案都不可能完美, 在成本和收益中做出取舍, 选择出最适合自己的方案即可, 在没有更好的方案出来之前, 只有继续忍耐它的不完美, 或者自己作出改变
 * 既然选择, 就不要抱怨, 感谢 今日头条技术团队 和 张鸿洋 等人对 Android 屏幕适配领域的的贡献
 * <p>
 * Created by JessYan on 2018/8/8 19:20
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * ================================================
 */
public final class AutoSize {

    static HiLogLabel label = new HiLogLabel(HiLog.LOG_APP, 0x0, "MYLOG");
    private static Map<Integer, DisplayMetricsInfo> mCache = new HashMap<>();
    private static final int MODE_SHIFT = 30;
    private static final int MODE_MASK  = 0x3 << MODE_SHIFT;
    private static final int MODE_ON_WIDTH  = 1 << MODE_SHIFT;
    private static final int MODE_DEVICE_SIZE  = 2 << MODE_SHIFT;

    private AutoSize() {
        throw new IllegalStateException("you can't instantiate me!");
    }

    /**
     * 检查 AndroidAutoSize 是否已经初始化
     *
     * @return {@code false} 表示 AndroidAutoSize 还未初始化, {@code true} 表示 AndroidAutoSize 已经初始化
     */
    public static boolean checkInit() {
        return AutoSizeConfig.getInstance().getInitDensity() != -1;
    }

    /**
     * 由于 AndroidAutoSize 会通过 {@link_TODO InitProvider} 的实例化而自动完成初始化, 并且 {@link_TODO AutoSizeConfig#init(Application)}
     * 只允许被调用一次, 否则会报错, 所以 {@link_TODO AutoSizeConfig#init(Application)} 的调用权限并没有设为 public, 不允许外部使用者调用
     * 但由于某些 issues 反应, 可能会在某些特殊情况下出现 {@link_TODO InitProvider} 未能正常实例化的情况, 导致 AndroidAutoSize 未能完成初始化
     * 所以提供此静态方法用于让外部使用者在异常情况下也可以初始化 AndroidAutoSize, 在 {@link_TODO Application#onCreate()} 中调用即可
     *
     * @param application {@link_TODO Application}
     */
    public static void checkAndInit(AbilityPackage application) {
        if (!checkInit()) {
            AutoSizeConfig.getInstance()
                    .setLog(true)
                    .init(application)
                    .setUseDeviceSize(false);
        }
    }

    /**
     * 使用 AndroidAutoSize 初始化时设置的默认适配参数进行适配 (AndroidManifest 的 Meta 属性)
     *
     * @param activity {@link_TODO Activity}
     */
    public static void autoConvertDensityOfGlobal(Ability activity) {
        if (AutoSizeConfig.getInstance().isBaseOnWidth()) {
            autoConvertDensityBaseOnWidth(activity, AutoSizeConfig.getInstance().getDesignWidthInDp());
        } else {
            autoConvertDensityBaseOnHeight(activity, AutoSizeConfig.getInstance().getDesignHeightInDp());
        }
    }

    /**
     * 使用 {@link_TODO Activity} 或 Fragment 的自定义参数进行适配
     *
     * @param activity    {@link_TODO Activity}
     * @param customAdapt {@link_TODO Activity} 或 Fragment 需实现 {@link_TODO CustomAdapt}
     */
    public static void autoConvertDensityOfCustomAdapt(Ability activity, CustomAdapt customAdapt) {
        Preconditions.checkNotNull(customAdapt, "customAdapt == null");
        float sizeInDp = customAdapt.getSizeInDp();

        //如果 CustomAdapt#getSizeInDp() 返回 0, 则使用在 AndroidManifest 上填写的设计图尺寸
        if (sizeInDp <= 0) {
            if (customAdapt.isBaseOnWidth()) {
                sizeInDp = AutoSizeConfig.getInstance().getDesignWidthInDp();
            } else {
                sizeInDp = AutoSizeConfig.getInstance().getDesignHeightInDp();
            }
        }
        autoConvertDensity(activity, sizeInDp, customAdapt.isBaseOnWidth());
    }

    /**
     * 使用外部三方库的 {@link_TODO Activity} 或 Fragment 的自定义适配参数进行适配
     *
     * @param activity          {@link_TODO Activity}
     * @param externalAdaptInfo 三方库的 {@link_TODO Activity} 或 Fragment 提供的适配参数, 需要配合 {@link_TODO ExternalAdaptManager#addExternalAdaptInfoOfActivity(Class, ExternalAdaptInfo)}
     */
    public static void autoConvertDensityOfExternalAdaptInfo(Ability activity, ExternalAdaptInfo externalAdaptInfo) {
        Preconditions.checkNotNull(externalAdaptInfo, "externalAdaptInfo == null");
        float sizeInDp = externalAdaptInfo.getSizeInDp();

        //如果 ExternalAdaptInfo#getSizeInDp() 返回 0, 则使用在 AndroidManifest 上填写的设计图尺寸
        if (sizeInDp <= 0) {
            if (externalAdaptInfo.isBaseOnWidth()) {
                sizeInDp = AutoSizeConfig.getInstance().getDesignWidthInDp();
            } else {
                sizeInDp = AutoSizeConfig.getInstance().getDesignHeightInDp();
            }
        }
        autoConvertDensity(activity, sizeInDp, externalAdaptInfo.isBaseOnWidth());
    }

    /**
     * 以宽度为基准进行适配
     *
     * @param activity        {@link_TODO Activity}
     * @param designWidthInDp 设计图的总宽度
     */
    public static void autoConvertDensityBaseOnWidth(Ability activity, float designWidthInDp) {
        autoConvertDensity(activity, designWidthInDp, true);
    }

    /**
     * 以高度为基准进行适配
     *
     * @param activity         {@link_TODO Activity}
     * @param designHeightInDp 设计图的总高度
     */
    public static void autoConvertDensityBaseOnHeight(Ability activity, float designHeightInDp) {
        autoConvertDensity(activity, designHeightInDp, false);
    }

    /**
     * 这里是今日头条适配方案的核心代码, 核心在于根据当前设备的实际情况做自动计算并转换 {@link_TODO DisplayMetrics#density}、
     * {@link_TODO DisplayMetrics#scaledDensity}、{@link_TODO DisplayMetrics#densityDpi} 这三个值, 额外增加 {@link_TODO DisplayMetrics#xdpi}
     * 以支持单位 {@code pt}、{@code in}、{@code mm}
     *
     * @param activity      {@link_TODO Activity}
     * @param sizeInDp      设计图上的设计尺寸, 单位 dp, 如果 {@param isBaseOnWidth} 设置为 {@code true},
     *                      {@param sizeInDp} 则应该填写设计图的总宽度, 如果 {@param isBaseOnWidth} 设置为 {@code false},
     *                      {@param sizeInDp} 则应该填写设计图的总高度
     * @param isBaseOnWidth 是否按照宽度进行等比例适配, {@code true} 为以宽度进行等比例适配, {@code false} 为以高度进行等比例适配
     * @see <a href="https://mp.weixin.qq.com/s/d9QCoBP6kV9VSWvVldVVwA">今日头条官方适配方案</a>
     */
    public static void autoConvertDensity(Ability activity, float sizeInDp, boolean isBaseOnWidth) {
        HiLog.info(label, "AutoSize.autoConvertDensity()");
        Preconditions.checkNotNull(activity, "activity == null");
        Preconditions.checkMainThread();

        float subunitsDesignSize = isBaseOnWidth ? AutoSizeConfig.getInstance().getUnitsManager().getDesignWidth()
                : AutoSizeConfig.getInstance().getUnitsManager().getDesignHeight();
        subunitsDesignSize = subunitsDesignSize > 0 ? subunitsDesignSize : sizeInDp;

        int screenSize = isBaseOnWidth ? AutoSizeConfig.getInstance().getScreenWidth()
                : AutoSizeConfig.getInstance().getScreenHeight();

        int key = Math.round((sizeInDp + subunitsDesignSize + screenSize) * AutoSizeConfig.getInstance().getInitScaledDensity()) & ~MODE_MASK;
        key = isBaseOnWidth ? (key | MODE_ON_WIDTH) : (key & ~MODE_ON_WIDTH);
        key = AutoSizeConfig.getInstance().isUseDeviceSize() ? (key | MODE_DEVICE_SIZE) : (key & ~MODE_DEVICE_SIZE);
        DisplayMetricsInfo displayMetricsInfo = mCache.get(key);

        float targetDensity = 0;
        int targetDensityDpi = 0;
        float targetScaledDensity = 0;
        float targetXdpi = 0;
        int targetScreenWidthDp;
        int targetScreenHeightDp;
        if (displayMetricsInfo == null) {
            if (isBaseOnWidth) {
                targetDensity = AutoSizeConfig.getInstance().getScreenWidth() * 1.0f / sizeInDp;
            } else {
                targetDensity = AutoSizeConfig.getInstance().getScreenHeight() * 1.0f / sizeInDp;
            }
            if (AutoSizeConfig.getInstance().getPrivateFontScale() > 0) {
                targetScaledDensity = targetDensity * AutoSizeConfig.getInstance().getPrivateFontScale();
            } else {
                float systemFontScale = AutoSizeConfig.getInstance().isExcludeFontScale() ? 1 : AutoSizeConfig.getInstance().
                        getInitScaledDensity() * 1.0f / AutoSizeConfig.getInstance().getInitDensity();
                targetScaledDensity = targetDensity * systemFontScale;
            }
            targetDensityDpi = (int) (targetDensity * 160);

            targetScreenWidthDp = (int) (AutoSizeConfig.getInstance().getScreenWidth() / targetDensity);
            targetScreenHeightDp = (int) (AutoSizeConfig.getInstance().getScreenHeight() / targetDensity);

            if (isBaseOnWidth) {
                targetXdpi = AutoSizeConfig.getInstance().getScreenWidth() * 1.0f / subunitsDesignSize;
            } else {
                targetXdpi = AutoSizeConfig.getInstance().getScreenHeight() * 1.0f / subunitsDesignSize;
            }

            mCache.put(key, new DisplayMetricsInfo(targetDensity, targetDensityDpi, targetScaledDensity, targetXdpi, targetScreenWidthDp, targetScreenHeightDp));
        } else {
            targetDensity = displayMetricsInfo.getDensity();
            targetDensityDpi = displayMetricsInfo.getDensityDpi();
            targetScaledDensity = displayMetricsInfo.getScaledDensity();
            targetXdpi = displayMetricsInfo.getXdpi();
            targetScreenWidthDp = displayMetricsInfo.getScreenWidthDp();
            targetScreenHeightDp = displayMetricsInfo.getScreenHeightDp();
        }
        HiLog.info(label, "targetDensity："+targetDensity);
        HiLog.info(label, "targetDensityDpi："+targetDensityDpi);
        HiLog.info(label, "targetScaledDensity："+targetScaledDensity);
        setDensity(activity, 3.5f, 560, targetScaledDensity, targetXdpi);
//        setScreenSizeDp(activity, targetScreenWidthDp, targetScreenHeightDp);

        AutoSizeLog.d(String.format(Locale.ENGLISH, "The %s has been adapted! \n%s Info: isBaseOnWidth = %s, %s = %f, %s = %f, targetDensity = %f, targetScaledDensity = %f, targetDensityDpi = %d, targetXdpi = %f, targetScreenWidthDp = %d, targetScreenHeightDp = %d"
                , activity.getClass().getName(), activity.getClass().getSimpleName(), isBaseOnWidth, isBaseOnWidth ? "designWidthInDp"
                        : "designHeightInDp", sizeInDp, isBaseOnWidth ? "designWidthInSubunits" : "designHeightInSubunits", subunitsDesignSize
                , targetDensity, targetScaledDensity, targetDensityDpi, targetXdpi, targetScreenWidthDp, targetScreenHeightDp));
        HiLog.info(label, "AutoSize.autoConvertDensity()over");
    }

    /**
     * 取消适配
     *
     * @param activity {@link_TODO Activity}
     */
    public static void cancelAdapt(Ability activity) {
        Preconditions.checkMainThread();
        float initXdpi = AutoSizeConfig.getInstance().getInitXdpi();
        switch (AutoSizeConfig.getInstance().getUnitsManager().getSupportSubunits()) {
            case PT:
                initXdpi = initXdpi / 72f;
                break;
            case MM:
                initXdpi = initXdpi / 25.4f;
                break;
            default:
        }
        setDensity(activity, AutoSizeConfig.getInstance().getInitDensity()
                , AutoSizeConfig.getInstance().getInitDensityDpi()
                , AutoSizeConfig.getInstance().getInitScaledDensity()
                , initXdpi);
//        setScreenSizeDp(activity
//                , AutoSizeConfig.getInstance().getInitScreenWidthDp()
//                , AutoSizeConfig.getInstance().getInitScreenHeightDp());
    }

    /**
     * 当 App 中出现多进程，并且您需要适配所有的进程，就需要在 App 初始化时调用 {@link_TODO #initCompatMultiProcess}
     * 建议实现自定义 {@link_TODO Application} 并在 {@link_TODO Application#onCreate()} 中调用 {@link_TODO #initCompatMultiProcess}
     *
     * @param context {@link_TODO Context}
     */
    public static void initCompatMultiProcess(Context context) {
        DataAbilityHelper helper = DataAbilityHelper.creator(context);
        try {
            helper.query(Uri.parse("dataability://localhost/.autosize-init-provider"), null, null);
        } catch (DataAbilityRemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给几大 {@link_TODO DisplayMetrics} 赋值
     *
     * @param activity      {@link_TODO Activity}
     * @param density       {@link_TODO DisplayMetrics#density}
     * @param densityDpi    {@link_TODO DisplayMetrics#densityDpi}
     * @param scaledDensity {@link_TODO DisplayMetrics#scaledDensity}
     * @param xdpi          {@link_TODO DisplayMetrics#xdpi}
     */
    private static void setDensity(Ability activity, float density, int densityDpi, float scaledDensity, float xdpi) {
        HiLog.info(label, "AutoSize.setDensity()1");
        DisplayAttributes activityDisplayMetrics = DisplayManager.getInstance()
                .getDefaultDisplay(activity.getContext()).get().getAttributes();
        setDensity(activityDisplayMetrics, density, densityDpi, scaledDensity, xdpi);

        DisplayAttributes appDisplayMetrics = DisplayManager.getInstance()
                .getDefaultDisplay(AutoSizeConfig.getInstance().getApplication().getContext()).get().getAttributes();
        setDensity(appDisplayMetrics, density, densityDpi, scaledDensity, xdpi);

        ResourceManager resource = activity.getResourceManager();
        DeviceCapability cap = resource.getDeviceCapability();
        cap.screenDensity = 100;
        Configuration config = resource.getConfiguration();
        activity.getResourceManager().updateConfiguration(config, cap);
        Display display = DisplayManager.getInstance()
                .getDefaultDisplay(activity.getContext()).get();
        HiLog.info(label, "real density:"+display.getRealAttributes().densityDpi);
        display.getRealAttributes().densityDpi = 100;
        display.getRealAttributes().densityPixels = 100;
        display.getRealAttributes().scalDensity = 100;
        HiLog.info(label, "density:"+display.getAttributes().densityDpi);
    }

    /**
     * 赋值
     *
     * @param displayMetrics {@link_TODO DisplayMetrics}
     * @param density        {@link_TODO DisplayMetrics#density}
     * @param densityDpi     {@link_TODO DisplayMetrics#densityDpi}
     * @param scaledDensity  {@link_TODO DisplayMetrics#scaledDensity}
     * @param xdpi           {@link_TODO DisplayMetrics#xdpi}
     */
    private static void setDensity(DisplayAttributes displayMetrics, float density, int densityDpi, float scaledDensity, float xdpi) {
        HiLog.info(label, "AutoSize.setDensity()2"+displayMetrics);
        displayMetrics.densityPixels = 3.5f;
        displayMetrics.densityDpi = 560;
        displayMetrics.xDpi = 560;
        if (AutoSizeConfig.getInstance().getUnitsManager().isSupportDP()) {
            displayMetrics.densityPixels = density;
            displayMetrics.densityDpi = densityDpi;
        }
        if (AutoSizeConfig.getInstance().getUnitsManager().isSupportSP()) {
            displayMetrics.scalDensity = scaledDensity;
        }
        switch (AutoSizeConfig.getInstance().getUnitsManager().getSupportSubunits()) {
            case NONE:
                break;
            case PT:
                displayMetrics.xDpi = xdpi * 72f;
                break;
            case IN:
                displayMetrics.xDpi = xdpi;
                break;
            case MM:
                displayMetrics.xDpi = xdpi * 25.4f;
                break;
            default:
        }
    }

    /**
     * 给 {@link_TODO Configuration} 赋值
     *
     * @param activity       {@link_TODO Activity}
     * @param screenWidthDp  {@link_TODO Configuration#screenWidthDp}
     * @param screenHeightDp {@link_TODO Configuration#screenHeightDp}
     */
//    private static void setScreenSizeDp(Ability activity, int screenWidthDp, int screenHeightDp) {
//        if (AutoSizeConfig.getInstance().getUnitsManager().isSupportDP() && AutoSizeConfig.getInstance().getUnitsManager().isSupportScreenSizeDP()) {
//            ConfigManager activityConfiguration = activity.getResourceManager().getConfigManager();
//            setScreenSizeDp(activityConfiguration, screenWidthDp, screenHeightDp);
//
//            Configuration appConfiguration = AutoSizeConfig.getInstance().getApplication().getResources().getConfiguration();
//            setScreenSizeDp(appConfiguration, screenWidthDp, screenHeightDp);
//        }
//    }

    /**
     * Configuration赋值
     *
     * @param configuration  {@link_TODO Configuration}
     * @param screenWidthDp  {@link_TODO Configuration#screenWidthDp}
     * @param screenHeightDp {@link_TODO Configuration#screenHeightDp}
     */
//    private static void setScreenSizeDp(Configuration configuration, int screenWidthDp, int screenHeightDp) {
//        configuration.screenWidthDp = screenWidthDp;
//        configuration.screenHeightDp = screenHeightDp;
//    }

}
