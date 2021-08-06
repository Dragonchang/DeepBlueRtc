package com.deepblue.rtccall.ims;

import org.java_websocket.handshake.ServerHandshake;

public interface ImServerConnectStateCallBack {
    /**
     * ws 连接到ims
     * @param serverHandshake
     */
    void onOpen(ServerHandshake serverHandshake);

    /**
     * ws 断开ims的连接
     * @param i
     * @param s
     * @param b
     */
    void onClose(int i, String s, boolean b);

    /**
     * ws 和ims连接发送错误
     * @param e
     */
    void onError(Exception e);
}
