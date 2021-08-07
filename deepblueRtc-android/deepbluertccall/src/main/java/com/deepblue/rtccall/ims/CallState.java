package com.deepblue.rtccall.ims;

/**
 * 视频通话状态
 */
public enum CallState {
    /**
     *  空闲
     */
    IDLE,

    /**
     * 呼叫中
     */
    OUTGOING,

    /**
     * 有通话到来
     */
    INCOMING,

    /**
     * 通话中
     */
    DIALING;
}
