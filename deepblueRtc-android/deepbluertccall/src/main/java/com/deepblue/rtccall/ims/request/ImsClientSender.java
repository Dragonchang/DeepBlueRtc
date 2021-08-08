package com.deepblue.rtccall.ims.request;

import android.app.Application;
import android.util.Log;

import com.deepblue.rtccall.bean.UserBean;
import com.deepblue.rtccall.consts.Urls;
import com.deepblue.rtccall.ims.ImServerCallBack;
import com.deepblue.webrtcpeer.rtc_comm.ws.DefaultSocketService;
import com.deepblue.webrtcpeer.rtc_comm.ws.SocketService;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * 发送消息到信令服务
 */
public class ImsClientSender {
    private static final String TAG = "ImsClient";
    /**
     * 和im信令服务的ws连接
     */
    private SocketService mIMServer;

    public ImsClientSender(Application application) {
        this.mIMServer =new DefaultSocketService(application);
    }

    /**
     * 连接ims
     * @param imsMessageCallBack
     */
    public void connectIMS(ImServerCallBack imsMessageCallBack) {
        if(mIMServer == null) {
            Log.e(TAG,"send client is null");
            return;
        }
        this.mIMServer.connect(Urls.IM_SERVER_WS_HOST, imsMessageCallBack);
    }

    public void register(UserBean localUser) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "register");
            obj.put("name", localUser.getName());

            mIMServer.sendMessage(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "register failed");
        }
        Log.i(TAG, "register success");
    }

    public void sendCallOfferSdp(SessionDescription sdp, UserBean localUserBean, UserBean remoteUserBean) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "call");
            obj.put("from", localUserBean.getName());
            obj.put("to", remoteUserBean.getName());
            obj.put("sdpOffer", sdp.description);
            mIMServer.sendMessage(obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendIncomingOfferSdp(SessionDescription sdp, UserBean remoteUserBean) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "incomingCallResponse");
            obj.put("from", remoteUserBean.getName());
            obj.put("callResponse", "accept");
            obj.put("sdpOffer", sdp.description);
            mIMServer.sendMessage(obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendLocalIceCandidate(IceCandidate iceCandidate) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "onIceCandidate");
            JSONObject candidate = new JSONObject();
            candidate.put("candidate", iceCandidate.sdp);
            candidate.put("sdpMid", iceCandidate.sdpMid);
            candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            obj.put("candidate", candidate);

            mIMServer.sendMessage(obj.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendStopCall() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "stop");
            mIMServer.sendMessage(obj.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
