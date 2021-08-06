package com.deepblue.rtccall.ims;

/**
 * 维护当前通话状态
 */
public class CallStateManager {
    /**
     * 当前通话状态
     */
    private CallState callState = CallState.IDLE;

    public void setCallState(CallState callState) {
        this.callState = callState;
    }

    public CallState getCallState() {
        return this.callState;
    }

    /**
     * 是否处于空闲状态
     * @return
     */
    public boolean isIdle() {
        return callState.equals(CallState.IDLE);
    }

    /**
     * 是否处于通过状态
     * @return
     */
    public boolean isDialing() {
        return callState.equals(CallState.DIALING);
    }
}
