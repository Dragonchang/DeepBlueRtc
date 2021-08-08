package com.deepblue.rtccall.rtc;

import android.content.Context;
import android.util.Log;

import com.deepblue.rtccall.consts.Urls;
import com.deepblue.rtccall.ui.ViewEntity;
import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionClient;
import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionParameters;
import com.deepblue.webrtcpeer.rtc_peer.SignalingParameters;
import com.deepblue.webrtcpeer.rtc_plugins.RTCAudioManager;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;

import java.util.LinkedList;

/**
 * Kurento rtc 引擎
 */
public class KurentoRTCEngine implements RTCEngine, PeerConnectionClient.PeerConnectionEvents{
    private static final String TAG = "KurentoRTCEngine";
    private PeerConnectionClient peerConnectionClient;
    private RTCAudioManager audioManager;
    private Context mContext;
    private SignalingParameters signalingParameters;
    private ViewEntity view;
    private PeerConnectionCallBack mPeerConnectionCallBack;
    private PeerConnectionParameters mPeerConnectionParameters;
    private boolean iceConnected;
    private RTCEngineConfig mRtcEngineConfig;


    public KurentoRTCEngine(Context context, ViewEntity view,
                            PeerConnectionCallBack peerConnectionCallBack, RTCEngineConfig rtcEngineConfig) {
        this.mContext = context;
        this.view = view;
        this.mPeerConnectionCallBack = peerConnectionCallBack;
        this.mRtcEngineConfig = rtcEngineConfig;
    }

    @Override
    public void createPeerConnectionFactory(final PeerConnectionParameters peerConnectionParameters) {
        mPeerConnectionParameters = peerConnectionParameters;
        peerConnectionClient = PeerConnectionClient.getInstance();
        peerConnectionClient.createPeerConnectionFactory(mContext, peerConnectionParameters, this);
        signalingParameters = new SignalingParameters(
                new LinkedList<PeerConnection.IceServer>() {
                    {
                        add(new PeerConnection.IceServer(Urls.STUN_SERVER_URL, Urls.STUN_USER, Urls.STUN_PASSWORD));
                    }
                }, true, null, null, null, null, null);
    }

    @Override
    public void startCall() {
        VideoCapturer videoCapturer = view.createVideoCapturer();

        peerConnectionClient
                .createPeerConnection(view.getEglBaseContext(), view.getLocalProxyRenderer(),
                        view.getRemoteProxyRenderer(), videoCapturer,
                        signalingParameters);
        peerConnectionClient.createOffer();

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = RTCAudioManager.create(mContext);
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start((audioDevice, availableAudioDevices) ->
                Log.d(TAG, "onAudioManagerDevicesChanged: " + availableAudioDevices + ", "
                        + "selected: " + audioDevice));

    }

    @Override
    public void inComingCall() {

    }

    @Override
    public void disconnect() {
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
    }

    public RTCEngineConfig getRTCEngineConfig() {
        return mRtcEngineConfig;
    }

    public void setRemoteDescription(final SessionDescription sdp) {
        peerConnectionClient.setRemoteDescription(sdp);
    }

    public void addRemoteIceCandidate(final IceCandidate candidate) {
        peerConnectionClient.addRemoteIceCandidate(candidate);
    }

    @Override
    public void onLocalDescription(SessionDescription sdp) {
        Log.e(TAG, "onLocalDescription ");
        mPeerConnectionCallBack.onLocalDescription(sdp);
        if (mPeerConnectionParameters.videoMaxBitrate > 0) {
            Log.d(TAG, "Set video maximum bitrate: " + mPeerConnectionParameters.videoMaxBitrate);
            peerConnectionClient.setVideoMaxBitrate(mPeerConnectionParameters.videoMaxBitrate);
        }
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.e(TAG, "onIceCandidate ");
        mPeerConnectionCallBack.onIceCandidate(candidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.e(TAG, "onIceCandidatesRemoved ");
        mPeerConnectionCallBack.onIceCandidatesRemoved(candidates);
    }

    @Override
    public void onIceConnected() {
        Log.i(TAG, "onIceConnected");
        iceConnected = true;
        mPeerConnectionCallBack.onIceConnected();
        peerConnectionClient.enableStatsEvents(true, 1000);
    }

    @Override
    public void onIceDisconnected() {
        Log.e(TAG, "onIceDisconnected");
        iceConnected = false;
        mPeerConnectionCallBack.onIceDisconnected();
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
    }

    @Override
    public void onPeerConnectionClosed() {
        Log.e(TAG, "onPeerConnectionClosed ");
        mPeerConnectionCallBack.onPeerConnectionClosed();
    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
        Log.e(TAG, "onPeerConnectionStatsReady: " + reports);
        mPeerConnectionCallBack.onPeerConnectionStatsReady(reports);
    }

    @Override
    public void onPeerConnectionError(String description) {
        Log.e(TAG, "onPeerConnectionError: " + description);
        mPeerConnectionCallBack.onPeerConnectionError(description);
    }
}
