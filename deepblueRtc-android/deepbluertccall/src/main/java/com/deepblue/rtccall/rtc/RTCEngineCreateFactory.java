package com.deepblue.rtccall.rtc;

import android.content.Context;

import com.deepblue.rtccall.ui.ViewEntity;

public class RTCEngineCreateFactory {

    public static KurentoRTCEngine CreateRTCEngine(Context context,
                                                   ViewEntity view,
                                                   PeerConnectionCallBack peerConnectionCallBack,
                                                   RTCEngineConfig rtcEngineConfig) {
        return new KurentoRTCEngine(context, view, peerConnectionCallBack, rtcEngineConfig);
    }
}
