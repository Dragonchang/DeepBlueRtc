package com.deepblue.rtccall.rtc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import com.deepblue.librtmp.RtmpReceiver;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class AVDecorder {
    private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

    private RtmpReceiver mRtmpReceiver;
    private RenderingCallBack mRenderingCallBack;

    private static final String VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private MediaCodec mDecoder;
    private MediaFormat videoFormat;
    private Thread videoDecoderThread;
    private volatile boolean videoDecoderLoop = false;
    private LinkedBlockingQueue<byte[]> videoQueue;
    private volatile boolean vDecoderEnd = false;

    public AVDecorder(RenderingCallBack renderingCallBack){
        mRtmpReceiver = RtmpReceiver.newInstance();
        mRenderingCallBack = renderingCallBack;
        mRtmpReceiver.init("rtmp://10.16.35.160/live/livestream", "/sdcard/log.text");
        initVideoDecoder();
        startVideoDecode();
    }

    public void startReadRtmpFrame() {
        if(mRtmpReceiver != null) {
            byte[] frame = mRtmpReceiver.readFrame();
            Log.w("AVDecorder", "startReadRtmpFrame &&&&&&&&&&&&&&&&"+frame);
            if(frame != null && frame.length >0) {
                Log.w("AVDecorder", "startReadRtmpFrame 222222222222222"+frame);
                putVideoData(frame);
            }
        }
    }

    /**
     * 添加视频数据
     *
     * @param data
     */
    public void putVideoData(byte[] data) {
        try {
            videoQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopVideoEncode() {
        Log.d("AVDecorder", "======zhongjihao======stop video 编码...");
        vDecoderEnd = true;
    }

    private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        Log.d("AVDecorder","supported color format: ");
        for (int c : caps.colorFormats) {
            Log.d("AVDecorder",c + "\t");
        }
        System.out.println();
    }


    private void initVideoDecoder() {
        videoQueue = new LinkedBlockingQueue<>();
        MediaCodecInfo vCodecInfo = selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC);
        if (vCodecInfo == null) {
            Log.e("AVDecorder", "====zhongjihao=====Unable to find an appropriate codec for " + VIDEO_MIME_TYPE);
            return;
        }
        try {
            //创建一个MediaCodec

            mDecoder = MediaCodec.createByCodecName(vCodecInfo.getName());
            Log.d("AVDecorder"," vCodecInfo name: "+vCodecInfo.getName());
//            showSupportedColorFormat(mDecoder.getCodecInfo().getCapabilitiesForType(VIDEO_MIME_TYPE));
//            if (isColorFormatSupported(decodeColorFormat, mDecoder.getCodecInfo().getCapabilitiesForType(VIDEO_MIME_TYPE))) {
//                //videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
//                Log.i("AVDecorder", "set decode color format to type " + decodeColorFormat);
//            } else {
//                Log.i("AVDecorder", "unable to set decode color format, color format type " + decodeColorFormat + " not supported");
//            }
            videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 848, 480);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,800000);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 26);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mDecoder.configure(videoFormat, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mDecoder.start();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("===zhongjihao===初始化视频编码器失败", e);
        }
    }

    private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            if (c == colorFormat) {
                return true;
            }
        }
        return false;
    }

    private void startVideoDecode() {
        if (mDecoder == null) {
            throw new RuntimeException("====zhongjihao=====请初始化视频编码器=====");
        }

        if (videoDecoderLoop) {
            throw new RuntimeException("====zhongjihao====视频编码必须先停止===");
        }
        videoDecoderThread = new Thread() {
            @Override
            public void run() {
                Log.d("AVDecorder", "===zhongjihao=====Video 编码线程 启动...");
                while (videoDecoderLoop && !Thread.interrupted()) {
                    try {
                        byte[] data = videoQueue.take(); //待编码的数据
                        Log.d("AVDecorder", "======zhongjihao====要编码的Video数据大小:" + data.length);
                        decodeVideoData(data);
                    } catch (InterruptedException e) {
                        Log.e("AVDecorder", "===zhongjihao==========编码(Video)数据 失败");
                        e.printStackTrace();
                        break;
                    }
                }

                if (mDecoder != null) {
                    //停止视频编码器
                    mDecoder.stop();
                    //释放视频编码器
                    mDecoder.release();
                    mDecoder = null;
                }
                videoQueue.clear();
                Log.d("AVDecorder", "=====zhongjihao======Video 编码线程 退出...");
            }
        };
        videoDecoderLoop = true;
        videoDecoderThread.start();
    }

    private void decodeVideoData(byte[] input) {

    }

    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}
