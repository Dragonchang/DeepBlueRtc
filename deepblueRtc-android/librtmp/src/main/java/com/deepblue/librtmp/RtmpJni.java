package com.deepblue.librtmp;

import android.util.Log;


public class RtmpJni {
    static {
        System.loadLibrary("rtmpJni");
    }

    public static final native long initRtmp(String url, String logpath);

    public static final native byte[] receiveVideoFrame(long cptr);

    public static final native int sendSpsAndPps(long cptr,byte[] sps, int spsLen, byte[] pps, int ppsLen);

    public static final native int sendVideoFrame(long cptr,byte[] frame, int len, int timestamp);

    public static final native int sendAacSpec(long cptr,byte[] data, int len);

    public static final native int sendAacData(long cptr,byte[] data, int len, int timestamp);

    public static final native int stopRtmp(long cptr);
}
