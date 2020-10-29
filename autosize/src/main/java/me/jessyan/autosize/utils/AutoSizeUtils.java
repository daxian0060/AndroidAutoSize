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
package me.jessyan.autosize.utils;


import ohos.aafwk.ability.AbilityPackage;
import ohos.agp.window.service.DisplayAttributes;
import ohos.agp.window.service.DisplayManager;
import ohos.app.Context;

import java.lang.reflect.InvocationTargetException;

/**
 * ================================================
 * AndroidAutoSize 常用工具类
 * <p>
 * Created by JessYan on 2018/8/25 15:24
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * ================================================
 */
public class AutoSizeUtils {

    /** {@link_TODO #TYPE_DIMENSION} complex unit: Value is raw pixels. */
    public static final int COMPLEX_UNIT_PX = 0;
    /** {@link_TODO #TYPE_DIMENSION} complex unit: Value is Device Independent
     *  Pixels. */
    public static final int COMPLEX_UNIT_DIP = 1;
    /** {@link_TODO #TYPE_DIMENSION} complex unit: Value is a scaled pixel. */
    public static final int COMPLEX_UNIT_SP = 2;
    /** {@link_TODO #TYPE_DIMENSION} complex unit: Value is in points. */
    public static final int COMPLEX_UNIT_PT = 3;
    /** {@link_TODO #TYPE_DIMENSION} complex unit: Value is in inches. */
    public static final int COMPLEX_UNIT_IN = 4;
    /** {@link_TODO #TYPE_DIMENSION} complex unit: Value is in millimeters. */
    public static final int COMPLEX_UNIT_MM = 5;

    private AutoSizeUtils() {
        throw new IllegalStateException("you can't instantiate me!");
    }

    public static int dp2px(Context context, float value) {
        DisplayAttributes activityDisplayMetrics = DisplayManager.getInstance()
                .getDefaultDisplay(context).get().getAttributes();
        return (int) (applyDimension(COMPLEX_UNIT_DIP, value, activityDisplayMetrics) + 0.5f);
    }

    public static int sp2px(Context context, float value) {
        DisplayAttributes activityDisplayMetrics = DisplayManager.getInstance()
                .getDefaultDisplay(context).get().getAttributes();
        return (int) (applyDimension(COMPLEX_UNIT_SP, value, activityDisplayMetrics) + 0.5f);
    }

    public static int pt2px(Context context, float value) {
        DisplayAttributes activityDisplayMetrics = DisplayManager.getInstance()
                .getDefaultDisplay(context).get().getAttributes();
        return (int) (applyDimension(COMPLEX_UNIT_PT, value, activityDisplayMetrics) + 0.5f);
    }

    public static int in2px(Context context, float value) {
        DisplayAttributes activityDisplayMetrics = DisplayManager.getInstance()
                .getDefaultDisplay(context).get().getAttributes();
        return (int) (applyDimension(COMPLEX_UNIT_IN, value, activityDisplayMetrics) + 0.5f);
    }

    public static int mm2px(Context context, float value) {
        DisplayAttributes activityDisplayMetrics = DisplayManager.getInstance()
                .getDefaultDisplay(context).get().getAttributes();
        return (int) (applyDimension(COMPLEX_UNIT_MM, value, activityDisplayMetrics) + 0.5f);
    }

    /**
     * TODO   android.app.ActivityThread
     * currentActivityThread
     * @return
     */
    public static AbilityPackage getApplicationByReflect() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            Object app = activityThread.getMethod("getApplication").invoke(thread);
            if (app == null) {
                throw new NullPointerException("you should init first");
            }
            return (AbilityPackage) app;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new NullPointerException("you should init first");
    }

    public static float applyDimension(int unit, float value, DisplayAttributes metrics) {
        switch (unit) {
            case COMPLEX_UNIT_PX:
                return value;
            case COMPLEX_UNIT_DIP:
                return value * metrics.densityDpi;
            case COMPLEX_UNIT_SP:
                return value * metrics.scalDensity;
            case COMPLEX_UNIT_PT:
                return value * metrics.xDpi * (1.0f/72);
            case COMPLEX_UNIT_IN:
                return value * metrics.xDpi;
            case COMPLEX_UNIT_MM:
                return value * metrics.xDpi * (1.0f/25.4f);
        }
        return 0;
    }
}
