package com.deepblue.librtmp;

import android.util.Log;

public class RtmpReceiver {
    private static final String TAG = "RtmpReceiver";
    private long cPtr;
    private Boolean mConnect;
    private String mUrl;
    private String mRtmpLogPath;
    private  int frameWidth;
    private  int frameHeight;
    private  int frameSize;
    private RtmpReceiver(){
        cPtr = 0;
        mConnect = false;
    }

    public static RtmpReceiver newInstance() {
        return new RtmpReceiver();
    }

    public int init(String url, String rtmpLogPath) {
        //初始化
        mUrl = url;
        mRtmpLogPath = rtmpLogPath;
        long ret =  RtmpJni.initRtmp(url, rtmpLogPath);
        if(ret == 0) {
            Log.e(TAG, "Rtmp连接失败====");
            mConnect = false;
            return 0;
        }
        cPtr = ret;
        mConnect = true;
        return 1;
    }


    public byte[] readFrame() {
        if(mConnect == false) {
            if(init(mUrl, mRtmpLogPath) == 0) {
                Log.e(TAG, "readFrame Rtmp连接失败====");
                return null;
            }
        }
        return RtmpJni.receiveVideoFrame(cPtr);
    }
}
