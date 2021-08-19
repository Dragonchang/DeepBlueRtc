package com.deepblue.rtccall.rtc;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;

import com.deepblue.librtmp.RtmpReceiver;

import org.webrtc.Logging;
import org.webrtc.VideoCodecStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class AVDecorder {
    private static final String TAG = "AVDecorder";
    private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;

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
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
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
                Log.d("AVDecorder", "===zhongjihao=====Video 解码线程 启动...");
                while (videoDecoderLoop && !Thread.interrupted()) {
                    try {
                        byte[] data = videoQueue.take(); //待解码的数据
                        Log.d("AVDecorder", "======zhongjihao====要解码的Video数据大小:" + data.length);
                        decodeVideoData(data);
                    } catch (InterruptedException e) {
                        Log.e("AVDecorder", "===zhongjihao==========解码(Video)数据 失败");
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
        int index;
        try {
            index = this.mDecoder.dequeueInputBuffer(500000L);
        } catch (IllegalStateException var11) {
            Log.e("AVDecorder", "dequeueInputBuffer failed", var11);
            return ;
        }
        Log.e("AVDecorder", "dequeueInputBuffer index:" +index);
        if (index < 0) {
            Log.e("AVDecorder", "decode() - no HW buffers available; decoder falling behind");
            return;
        } else {
            ByteBuffer buffer;
            try {
                buffer = this.mDecoder.getInputBuffers()[index];
            } catch (IllegalStateException var10) {
                Log.e("AVDecorder", "getInputBuffers failed", var10);
                return;
            }
            if (buffer.capacity() < input.length) {
                Log.e("AVDecorder", "decode() - HW buffer too small");
                return ;
            } else {
                buffer.put(input);
                try {
                    this.mDecoder.queueInputBuffer(index, 0, input.length, SystemClock.elapsedRealtime() * 1000L, 0);
                    deliverDecodedFrame();
                } catch (IllegalStateException var9) {
                    Log.e("AVDecorder", "queueInputBuffer failed", var9);
                    return ;
                }
            }
        }
    }

    private void deliverDecodedFrame() {
        try {
            Log.e("AVDecorder", "deliverDecodedFrame*****************");
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int result = this.mDecoder.dequeueOutputBuffer(info, 100000L);
            if (result == -2) {
                Log.e("AVDecorder", "deliverDecodedFrame*11111111111111111111");
                //this.reformat(this.codec.getOutputFormat());
                return;
            }

            if (result < 0) {
                Log.v("AVDecorder", "dequeueOutputBuffer returned " + result);
                return;
            }
            boolean doRender = (info.size != 0);
            if (doRender) {
                Image image = this.mDecoder.getOutputImage(result);
                if(image == null) {
                    Log.e("AVDecorder", "image : null" );
                    return;
                }
                Log.e("AVDecorder", "image format: " + image.getFormat());
                byte[] arr =getDataFromImage(image, COLOR_FormatNV21);
                if(arr != null && arr.length > 0) {
                    mRenderingCallBack.renderFrame(arr);
                }
            } else {
                Log.e("AVDecorder", "deliverDecodedFrame*222222222222222222222");
            }
        } catch (IllegalStateException var16) {
            Log.e("AVDecorder", "deliverDecodedFrame failed", var16);
        }
    }

    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        Log.v(TAG, "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            Log.v(TAG, "pixelStride " + pixelStride);
            Log.v(TAG, "rowStride " + rowStride);
            Log.v(TAG, "width " + width);
            Log.v(TAG, "height " + height);
            Log.v(TAG, "buffer size " + buffer.remaining());

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
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
