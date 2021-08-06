package com.deepblue.rtccall.rtc;

import android.content.Context;

import com.deepblue.rtccall.ui.ViewEntity;

public class RTCEngineCreateFactory {

    public static RTCEngine CreateRTCEngine(Context context, ViewEntity view) {
        return new KurentoRTCEngine(context, view);
    }
}
