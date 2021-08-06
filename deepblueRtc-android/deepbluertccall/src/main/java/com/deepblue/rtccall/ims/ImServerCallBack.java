package com.deepblue.rtccall.ims;

import com.deepblue.rtccall.ims.response.ServerResponse;
import com.deepblue.webrtcpeer.rtc_comm.ws.BaseSocketCallback;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.java_websocket.handshake.ServerHandshake;

import java.util.ArrayList;
import java.util.List;

/**
 * ims 消息和连接状态回调
 */
public class ImServerCallBack extends BaseSocketCallback {
    private Gson mGson;

    //ims连接状态回调
    private List<ImServerConnectStateCallBack> mImsConnectCallBack = new ArrayList<>();
    //消息回调
    private List<ImServerMessageCallBack> imServerMessageCallBacks = new ArrayList<>();

    public ImServerCallBack() {
        this.mGson = new Gson();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        super.onOpen(serverHandshake);
        synchronized (mImsConnectCallBack) {
            for(ImServerConnectStateCallBack callBack : mImsConnectCallBack) {
                callBack.onOpen(serverHandshake);
            }
        }
    }

    @Override
    public void onMessage(String serverResponse_) {
        super.onMessage(serverResponse_);
        try {
            ServerResponse serverResponse = mGson.fromJson(serverResponse_, ServerResponse.class);

            switch (serverResponse.getIdRes()) {
                //ims 注册成功
                case REGISTER_RESPONSE:
                    synchronized (imServerMessageCallBacks) {
                        for(ImServerMessageCallBack callBack : imServerMessageCallBacks) {
                            callBack.imsRegisterResponse(serverResponse);
                        }
                    }
                    break;
                //有来电
                case INCOMING_CALL:
                    synchronized (imServerMessageCallBacks) {
                        for(ImServerMessageCallBack callBack : imServerMessageCallBacks) {
                            callBack.imsIncomingCall(serverResponse);
                        }
                    }
                    break;
                 //通话接通
                case CALL_RESPONSE:
                    synchronized (imServerMessageCallBacks) {
                        for(ImServerMessageCallBack callBack : imServerMessageCallBacks) {
                            callBack.imsCallResponse(serverResponse);
                        }
                    }

                    break;

                case ICE_CANDIDATE:
                    synchronized (imServerMessageCallBacks) {
                        for(ImServerMessageCallBack callBack : imServerMessageCallBacks) {
                            callBack.imsIceCandidate(serverResponse);
                        }
                    }
                    break;
                //来电开始通话
                case START_COMMUNICATION:
                    synchronized (imServerMessageCallBacks) {
                        for(ImServerMessageCallBack callBack : imServerMessageCallBacks) {
                            callBack.imsStartCommunication(serverResponse);
                        }
                    }
                    break;
                //结束通话
                case STOP_COMMUNICATION:
                    synchronized (imServerMessageCallBacks) {
                        for(ImServerMessageCallBack callBack : imServerMessageCallBacks) {
                            callBack.imsStopCommunication(serverResponse);
                        }
                    }
                    break;
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        super.onClose(i, s, b);
        synchronized (mImsConnectCallBack) {
            for(ImServerConnectStateCallBack callBack : mImsConnectCallBack) {
                callBack.onClose(i, s, b);
            }
        }
    }

    @Override
    public void onError(Exception e) {
        super.onError(e);
        synchronized (mImsConnectCallBack) {
            for(ImServerConnectStateCallBack callBack : mImsConnectCallBack) {
                callBack.onError(e);
            }
        }
    }

    /**
     * 注册ims连接状态callback
     * @param imServerConnectStateCallBack
     */
    public void registerImsConnectCallBack(ImServerConnectStateCallBack imServerConnectStateCallBack) {
        synchronized (mImsConnectCallBack) {
            if(!mImsConnectCallBack.contains(imServerConnectStateCallBack)) {
                mImsConnectCallBack.add(imServerConnectStateCallBack);
            }
        }
    }

    /**
     * 注销ims连接状态callback
     * @param imServerConnectStateCallBack
     */
    public void unRegisterImsConnectCallBack(ImServerConnectStateCallBack imServerConnectStateCallBack) {
        synchronized (mImsConnectCallBack) {
            if(!mImsConnectCallBack.contains(imServerConnectStateCallBack)) {
                mImsConnectCallBack.remove(imServerConnectStateCallBack);
            }
        }
    }

    /**
     * 注册ims消息callback
     * @param imServerMessageCallBack
     */
    public void registerImsMessageCallBack(ImServerMessageCallBack imServerMessageCallBack) {
        synchronized (imServerMessageCallBacks) {
            if(!imServerMessageCallBacks.contains(imServerMessageCallBack)) {
                imServerMessageCallBacks.add(imServerMessageCallBack);
            }
        }
    }

    /**
     * 注销ims消息callback
     * @param imServerMessageCallBack
     */
    public void unRegisterImsMessageCallBack(ImServerMessageCallBack imServerMessageCallBack) {
        synchronized (imServerMessageCallBacks) {
            if(!imServerMessageCallBacks.contains(imServerMessageCallBack)) {
                imServerMessageCallBacks.remove(imServerMessageCallBack);
            }
        }
    }
}
