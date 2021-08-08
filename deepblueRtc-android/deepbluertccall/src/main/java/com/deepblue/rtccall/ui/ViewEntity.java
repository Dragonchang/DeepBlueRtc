package com.deepblue.rtccall.ui;

import org.webrtc.EglBase;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

public interface ViewEntity {

    void setSwappedFeeds(boolean swappedFeed);

    VideoCapturer createVideoCapturer();

    EglBase.Context getEglBaseContext();

    EglBase getEglBase();

    VideoRenderer.Callbacks getLocalProxyRenderer();

    VideoRenderer.Callbacks getRemoteProxyRenderer();

    /**
     * 更新到拨打通话中状态
     */
    void updateToOutgoingStatus();

    /**
     * 更新到来电状态
     */
    void updateToIncomingCallStatus();

    /**
     * 更新到接听状态
     */
    void updateToDialingStatus();

    /**
     * 更新到挂断电话状态
     */
    void updateToHangUpStatus();

}
