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

import ohos.aafwk.ability.*;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

/**
 * ================================================
 * {@link_TODO ActivityLifecycleCallbacksImpl} 可用来代替在 BaseActivity 中加入适配代码的传统方式
 * {@link_TODO ActivityLifecycleCallbacksImpl} 这种方案类似于 AOP, 面向接口, 侵入性低, 方便统一管理, 扩展性强, 并且也支持适配三方库的 {@link_TODO Activity}
 * <p>
 * Created by JessYan on 2018/8/8 14:32
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * ================================================
 */
public class ActivityLifecycleCallbacksImpl implements AbilityLifecycleCallbacks {

    static HiLogLabel label = new HiLogLabel(HiLog.LOG_APP, 0x0, "MYLOG");
    /**
     * 屏幕适配逻辑策略类
     */
    private AutoAdaptStrategy mAutoAdaptStrategy;

    public ActivityLifecycleCallbacksImpl(AutoAdaptStrategy autoAdaptStrategy) {
        mAutoAdaptStrategy = autoAdaptStrategy;
    }

    @Override
    public void onAbilityStart(Ability ability) {
        HiLog.info(label, "ActivityLifecycleCallbacksImpl.onAbilityStart()");
        HiLog.info(label, "ability"+ability);
        HiLog.info(label, "mAutoAdaptStrategy"+mAutoAdaptStrategy);
        //Activity 中的 setContentView(View) 一定要在 super.onCreate(Bundle); 之后执行
        if (mAutoAdaptStrategy != null) {
            mAutoAdaptStrategy.applyAdapt(ability, ability);
        }
    }

    @Override
    public void onAbilityActive(Ability ability) {
        if (mAutoAdaptStrategy != null) {
            mAutoAdaptStrategy.applyAdapt(ability, ability);
        }
    }

    @Override
    public void onAbilityForeground(Ability ability) {

    }

    @Override
    public void onAbilityBackground(Ability ability) {

    }

    @Override
    public void onAbilityInactive(Ability ability) {

    }

    @Override
    public void onAbilityStop(Ability ability) {

    }

    /**
     * 设置屏幕适配逻辑策略类
     *
     * @param autoAdaptStrategy {@link AutoAdaptStrategy}
     */
    public void setAutoAdaptStrategy(AutoAdaptStrategy autoAdaptStrategy) {
        mAutoAdaptStrategy = autoAdaptStrategy;
    }
}
