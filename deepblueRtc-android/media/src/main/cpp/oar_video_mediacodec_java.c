/*
 The MIT License (MIT)

Copyright (c) 2017-2020 oarplayer(qingkouwei)

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
#define _JNILOG_TAG "videomediacodec"
#include "_android.h"

#include <unistd.h>
#include <malloc.h>
#include "oar_video_mediacodec_java.h"
#include "oarplayer_type_def.h"
#include "util.h"
#include "oar_video_mediacodec_ctx.h"

#define _LOGD  LOGE


void oar_video_mediacodec_start(oarplayer *oar){
    _LOGD("oar_video_mediacodec_start...");
    oar_video_mediacodec_context *ctx = oar->video_mediacodec_ctx;
    JNIEnv *jniEnv = ctx->jniEnv;
    oar_java_class * jc = oar->jc;
    jobject codecName = NULL, csd_0 = NULL, csd_1 = NULL;
    while(oar->video_render_ctx->texture_window == NULL){
        usleep(10000);
    }
    switch (ctx->codec_id) {
        case VIDEO_CODEC_AVC:
            codecName = (*jniEnv)->NewStringUTF(jniEnv, "video/avc");
            if (oar->metadata->video_extradata) {
                size_t sps_size, pps_size;
                uint8_t *sps_buf;
                uint8_t *pps_buf;
                sps_buf = (uint8_t *) malloc((size_t) oar->metadata->video_extradata_size + 20);
                pps_buf = (uint8_t *) malloc((size_t) oar->metadata->video_extradata_size + 20);
                if (0 != convert_sps_pps2(oar->metadata->video_extradata, (size_t) oar->metadata->video_extradata_size,
                                          sps_buf, &sps_size, pps_buf, &pps_size, &ctx->nal_size)) {
                    LOGE("%s:convert_sps_pps: failed\n", __func__);
                }
                _LOGD("extradata_size = %d, sps_size = %d, pps_size = %d, nal_size = %d",
                     (size_t) oar->metadata->video_extradata_size, sps_size, pps_size, ctx->nal_size);

                csd_0 = (*jniEnv)->NewDirectByteBuffer(jniEnv, sps_buf, sps_size);
                csd_1 = (*jniEnv)->NewDirectByteBuffer(jniEnv, pps_buf, pps_size);
                (*jniEnv)->CallStaticVoidMethod(jniEnv, jc->HwDecodeBridge, jc->codec_init,
                                                codecName, ctx->width, ctx->height, csd_0, csd_1);
                (*jniEnv)->DeleteLocalRef(jniEnv, csd_0);
                (*jniEnv)->DeleteLocalRef(jniEnv, csd_1);
            } else {
                (*jniEnv)->CallStaticVoidMethod(jniEnv, jc->HwDecodeBridge, jc->codec_init,
                                                codecName, ctx->width, ctx->height, NULL, NULL);
            }
            break;

        case VIDEO_CODEC_H263:
            codecName = (*jniEnv)->NewStringUTF(jniEnv, "video/3gpp");
            //TODO 解析h263格式
            /*csd_0 = (*jniEnv)->NewDirectByteBuffer(jniEnv, oar->metadata->video_sps, oar->metadata->video_sps_size);
            (*jniEnv)->CallStaticVoidMethod(jniEnv, jc->HwDecodeBridge, jc->codec_init, codecName,
                                            ctx->width, ctx->height, csd_0, NULL);
            (*jniEnv)->DeleteLocalRef(jniEnv, csd_0);*/
            break;
        default:
            break;
    }
    if (codecName != NULL) {
        (*jniEnv)->DeleteLocalRef(jniEnv, codecName);
    }
}

void oar_video_mediacodec_release_buffer(oarplayer *oar, int index) {
    JNIEnv *jniEnv = oar->video_render_ctx->jniEnv;
    oar_java_class * jc = oar->jc;
    (*jniEnv)->CallStaticVoidMethod(jniEnv, jc->HwDecodeBridge, jc->codec_releaseOutPutBuffer,
                                    index);
}

int oar_video_mediacodec_receive_frame(oarplayer *oar, OARFrame *frame) {
    JNIEnv *jniEnv = oar->video_mediacodec_ctx->jniEnv;
    oar_java_class * jc = oar->jc;
    oar_video_mediacodec_context *ctx = oar->video_mediacodec_ctx;
    int output_ret = 1;
    jobject deqret = (*jniEnv)->CallStaticObjectMethod(jniEnv, jc->HwDecodeBridge,
                                                       jc->codec_dequeueOutputBufferIndex,
                                                       (jlong) 0);
    uint8_t *retbuf = (*jniEnv)->GetDirectBufferAddress(jniEnv, deqret);
    int outbufidx = get_int(retbuf);
    int64_t pts = get_long(retbuf + 8);
    (*jniEnv)->DeleteLocalRef(jniEnv, deqret);
    _LOGD("调用系统的硬件解码接口oar_video_mediacodec_receive_frame outbufidx:%d" , outbufidx);
    if (outbufidx >= 0) {
        frame->type = PktType_Video;
        frame->pts = pts;
        frame->format = PIX_FMT_EGL_EXT;
        frame->width = oar->metadata->width;
        frame->height = oar->metadata->height;
        frame->HW_BUFFER_ID = outbufidx;
        frame->next=NULL;
        output_ret = 0;
    } else {
        switch (outbufidx) {
            // AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED
            case -2: {
                jobject newFormat = (*jniEnv)->CallStaticObjectMethod(jniEnv, jc->HwDecodeBridge,
                                                                      jc->codec_formatChange);
                uint8_t *fmtbuf = (*jniEnv)->GetDirectBufferAddress(jniEnv, newFormat);
                ctx->width = get_int(fmtbuf);
                ctx->height = get_int(fmtbuf + 4);
                int pix_format = get_int(fmtbuf + 8);
                (*jniEnv)->DeleteLocalRef(jniEnv, newFormat);

                //todo 仅支持了两种格式
                switch (pix_format) {
                    case 19:
                        ctx->pix_format = PIX_FMT_YUV420P;
                        break;
                    case 21:
                        ctx->pix_format = PIX_FMT_NV12;
                        break;
                    default:
                        break;
                }
                output_ret = -2;
                break;
            }
            // AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED
            case -3:
                break;
            // AMEDIACODEC_INFO_TRY_AGAIN_LATER
            case -1:
                break;
            default:
                break;
        }

    }
    return output_ret;
}

int oar_video_mediacodec_send_packet(oarplayer *oar, OARPacket *packet) {
    JNIEnv *jniEnv = oar->video_mediacodec_ctx->jniEnv;
    oar_java_class * jc = oar->jc;
    oar_video_mediacodec_context *ctx = oar->video_mediacodec_ctx;
    if (packet == NULL) { return -2; }
    int keyframe_flag = 0;
    int64_t time_stamp = packet->pts;

    if (ctx->codec_id == VIDEO_CODEC_AVC) {
        H264ConvertState convert_state = {0, 0};
        convert_h264_to_annexb(packet->data, packet->size, ctx->nal_size, &convert_state);
    }
    if (packet->isKeyframe) {
        keyframe_flag |= 0x1;
    }
    int id = (*jniEnv)->CallStaticIntMethod(jniEnv, jc->HwDecodeBridge,
                                            jc->codec_dequeueInputBuffer, (jlong) 1000000);
    if (id >= 0) {
        jobject inputBuffer = (*jniEnv)->CallStaticObjectMethod(jniEnv, jc->HwDecodeBridge,
                                                                jc->codec_getInputBuffer, id);
        uint8_t *buf = (*jniEnv)->GetDirectBufferAddress(jniEnv, inputBuffer);
        jlong size = (*jniEnv)->GetDirectBufferCapacity(jniEnv, inputBuffer);
        if (buf != NULL && size >= packet->size) {
            memcpy(buf, packet->data, (size_t) packet->size);
            (*jniEnv)->CallStaticVoidMethod(jniEnv, jc->HwDecodeBridge,
                                            jc->codec_queueInputBuffer,
                                            (jint) id, (jint) packet->size,
                                            (jlong) time_stamp, (jint) keyframe_flag);
        }
        (*jniEnv)->DeleteLocalRef(jniEnv, inputBuffer);
    } else if (id == -1) {
        _LOGD("dequeue inputbuffer is -1");
        return -1;
    } else {
        _LOGD("input buffer id < 0  value == %zd", id);
    }
    return 0;
}

void oar_video_mediacodec_flush(oarplayer *oar) {
    JNIEnv *jniEnv = oar->video_mediacodec_ctx->jniEnv;
    oar_java_class * jc = oar->jc;
    (*jniEnv)->CallStaticVoidMethod(jniEnv, jc->HwDecodeBridge, jc->codec_flush);
}

void oar_video_mediacodec_release_context(oarplayer *oar) {
    JNIEnv *jniEnv = oar->jniEnv;
    oar_java_class * jc = oar->jc;
    (*jniEnv)->CallStaticVoidMethod(jniEnv, jc->HwDecodeBridge, jc->codec_release);
    oar_video_mediacodec_context *ctx = oar->video_mediacodec_ctx;
    free(ctx);
    oar->video_mediacodec_ctx = NULL;
}

void oar_video_mediacodec_stop(oarplayer *oar) {
    JNIEnv *jniEnv = oar->video_mediacodec_ctx->jniEnv;
    oar_java_class * jc = oar->jc;
    (*jniEnv)->CallStaticVoidMethod(jniEnv, jc->HwDecodeBridge, jc->codec_stop);
}

//#endif