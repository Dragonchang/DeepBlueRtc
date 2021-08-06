package com.deepblue.rtccall.rtc;

import android.content.Context;
import android.util.Log;

import com.deepblue.rtccall.consts.Urls;
import com.deepblue.rtccall.ui.ViewEntity;
import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionClient;
import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionParameters;
import com.deepblue.webrtcpeer.rtc_peer.SignalingParameters;
import com.deepblue.webrtcpeer.rtc_plugins.RTCAudioManager;

import org.webrtc.PeerConnection;
import org.webrtc.VideoCapturer;

import java.util.LinkedList;

/**
 * Kurento rtc 引擎
 */
public class KurentoRTCEngine implements RTCEngine{
    private static final String TAG = "KurentoRTCEngine";
    private PeerConnectionClient peerConnectionClient;
    private RTCAudioManager audioManager;
    private Context mContext;
    private SignalingParameters signalingParameters;
    ViewEntity view;

    public KurentoRTCEngine(Context context, ViewEntity view) {
        this.mContext = context;
        this.view = view;
    }

    @Override
    public void createPeerConnectionFactory(final PeerConnectionParameters peerConnectionParameters,
                                     final PeerConnectionClient.PeerConnectionEvents events) {
        peerConnectionClient = PeerConnectionClient.getInstance();
        peerConnectionClient.createPeerConnectionFactory(mContext, peerConnectionParameters, events);
        signalingParameters = new SignalingParameters(
                new LinkedList<PeerConnection.IceServer>() {
                    {
                        add(new PeerConnection.IceServer(Urls.STUN_SERVER_URL));
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
    public void disconnect() {
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
    }
}
