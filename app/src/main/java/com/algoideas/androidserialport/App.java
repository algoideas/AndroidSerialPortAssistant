package com.algoideas.androidserialport;

import android.app.Application;
import com.blankj.utilcode.util.Utils;
import net.danlew.android.joda.JodaTimeAndroid;

/**
 * author： algoideas
 * date:    2019/3/17
 * desc:
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(this);

        JodaTimeAndroid.init(this);
    }
}
