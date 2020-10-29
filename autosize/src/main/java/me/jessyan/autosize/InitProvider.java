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

import me.jessyan.autosize.utils.AutoSizeUtils;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.ability.AbilityPackage;
import ohos.aafwk.content.Intent;
import ohos.app.Context;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogConstString;
import ohos.hiviewdfx.HiLogLabel;

/**
 * ================================================
 * 通过声明 {@link_TODO ContentProvider} 自动完成初始化
 * Created by JessYan on 2018/8/19 11:55
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * ================================================
 */
public class InitProvider extends Ability {

    static HiLogLabel label = new HiLogLabel(HiLog.LOG_APP, 0x0, "MYLOG");

    @Override
    protected void onStart(Intent intent) {
        HiLog.info(label, "InitProvider.onStart()");
        super.onStart(intent);
        Context application = getContext().getApplicationContext();
//        if (application == null) {
//            application = AutoSizeUtils.getApplicationByReflect();
//        }
        AutoSizeConfig.getInstance()
                .setLog(true)
                .init((AbilityPackage) application)
                .setUseDeviceSize(false);
    }
}
