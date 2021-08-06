package com.deepblue.rtccall.ims;

import android.app.Application;
import android.util.Log;

import com.deepblue.rtccall.bean.UserBean;
import com.deepblue.rtccall.consts.Urls;
import com.deepblue.rtccall.rtc.RTCEngineCreateFactory;
import com.deepblue.rtccall.rtc.RTCEngine;
import com.deepblue.rtccall.ui.ViewEntity;
import com.deepblue.webrtcpeer.rtc_comm.ws.DefaultSocketService;
import com.deepblue.webrtcpeer.rtc_comm.ws.SocketService;
import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionClient;
import com.deepblue.webrtcpeer.rtc_peer.SignalingEvents;
import com.deepblue.webrtcpeer.rtc_peer.SignalingParameters;
import com.deepblue.webrtcpeer.rtc_peer.config.DefaultConfig;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.util.LinkedList;

/**
 * 视频通话管理器
 */
public class DeepBlueVideoCallManger implements SignalingEvents, PeerConnectionClient.PeerConnectionEvents{
    private static final String TAG = "videoCallManager";

    private Application application;

    /**
     * 和im信令服务的ws连接
     */
    private SocketService mIMServer;

    private ImServerCallBack mImsMessageCallBack;

    private CallStateManager mCallStateManger;

    private DefaultConfig mDefaultConfig;

    private RTCEngine mRTCEngine;

    private ViewEntity mView;

    private static volatile DeepBlueVideoCallManger singleton;

    public static DeepBlueVideoCallManger getInstance(Application application) {
        if (singleton == null) {
            synchronized (DeepBlueVideoCallManger.class) {
                if (singleton == null) {
                    singleton = new DeepBlueVideoCallManger(application);
                }
            }
        }
        return singleton;
    }

    private DeepBlueVideoCallManger(Application application) {
        this.application = application;
    }

    public void initIMServerManger(ViewEntity view) {
        this.mView = view;
        this.mIMServer = new DefaultSocketService(application);
        this.mImsMessageCallBack = new ImServerCallBack();
        this.mCallStateManger = new CallStateManager();
        this.mDefaultConfig = new DefaultConfig();
        this.mRTCEngine = RTCEngineCreateFactory.CreateRTCEngine(application.getApplicationContext(), view);
        connectIMServer();
    }

    /**
     * 连接IM服务 app启动之后调用
     * 网络断开重连调用
     */
    public void connectIMServer() {
        mIMServer.connect(Urls.IM_SERVER_WS_HOST, mImsMessageCallBack);
    }

    /**
     * 开始通话
     * @param localUserBean
     * @param remoteUserBean
     */
    public void StartCalling(UserBean localUserBean, UserBean remoteUserBean) {
        if(localUserBean == null || remoteUserBean == null) {
            Log.e(TAG, "StartCalling failed, reason: user info is null");
            return;
        }
        this.mRTCEngine.createPeerConnectionFactory(mDefaultConfig.createPeerConnectionParams(), this);

    }

    public DefaultConfig getDefaultConfig() {
        return mDefaultConfig;
    }

    @Override
    public void onLocalDescription(SessionDescription sdp) {

    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {

    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {

    }

    @Override
    public void onIceConnected() {
        Log.i(TAG, "onIceConnected");
    }

    @Override
    public void onIceDisconnected() {
        Log.e(TAG, "onIceDisconnected");
    }

    @Override
    public void onPeerConnectionClosed() {

    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
        Log.e(TAG, "run: " + reports);
    }

    @Override
    public void onPeerConnectionError(String description) {
        Log.e(TAG, "onPeerConnectionError: " + description);
    }





    //************************************signal*****************************************//
    @Override
    public void onSignalConnected(SignalingParameters params) {

    }

    @Override
    public void onRemoteDescription(SessionDescription sdp) {

    }

    @Override
    public void onRemoteIceCandidate(IceCandidate candidate) {

    }

    @Override
    public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {

    }

    @Override
    public void onChannelClose() {

    }

    @Override
    public void onChannelError(String description) {

    }
}
