package com.deepblue.rtccall.rtc;

import android.content.Context;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;

public class RtmpVideoCapture implements VideoCapturer {

    private CapturerObserver mCapturerObserver;
    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        mCapturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int i, int i1, int i2) {

    }

    @Override
    public void stopCapture() throws InterruptedException {

    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean isScreencast() {
        return false;
    }
}
