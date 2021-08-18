package com.deepblue.rtccall.rtc;

import android.content.Context;
import android.os.SystemClock;

import com.deepblue.librtmp.RtmpReceiver;

import org.webrtc.FileVideoCapturer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class RtmpVideoCapture implements VideoCapturer, RenderingCallBack{

    private CapturerObserver mCapturerObserver;

    private AVDecorder mAVDecorder;
    private final Timer timer = new Timer();
    private final TimerTask tickTask = new TimerTask() {
        public void run() {
            RtmpVideoCapture.this.tick();
        }
    };

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        mCapturerObserver = capturerObserver;
        mAVDecorder = new AVDecorder(this);
    }

    @Override
    public void startCapture(int i, int i1, int framerate) {
        this.timer.schedule(this.tickTask, 0L, (long)(1000 / framerate));
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


    public void tick() {
        startReceiveRtmpFrame();
    }

    private void startReceiveRtmpFrame() {
        mAVDecorder.startReadRtmpFrame();
    }

    @Override
    public void renderFrame(byte[] frame) {
        long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
        this.mCapturerObserver.onByteBufferFrameCaptured(frame, 848, 480, 0, captureTimeNs);
    }
}
