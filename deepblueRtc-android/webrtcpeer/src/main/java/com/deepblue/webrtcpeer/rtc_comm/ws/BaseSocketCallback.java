package com.deepblue.webrtcpeer.rtc_comm.ws;


import android.util.Log;
import org.java_websocket.handshake.ServerHandshake;


public class BaseSocketCallback implements SocketCallBack {
    private static final String TAG = "WebsocketIms";
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Log.i(TAG,"onOpen");
    }

    @Override
    public void onMessage(String serverResponse) {
        Log.i(TAG,"onMessage: " + serverResponse);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        Log.i(TAG,"onClose: " + s);
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
}
