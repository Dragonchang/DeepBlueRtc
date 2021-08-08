package com.deepblue.rtc.one2one;

import android.app.Application;
import android.util.Log;

import com.deepblue.rtc.rtc_peer.kurento.KurentoOne2OneRTCClient;
import com.deepblue.rtc.rtc_peer.kurento.models.CandidateModel;
import com.deepblue.rtc.rtc_peer.kurento.models.response.ServerResponse;
import com.deepblue.rtc.rtc_peer.kurento.models.response.TypeResponse;
import com.deepblue.webrtcpeer.rtc_plugins.RTCAudioManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import com.deepblue.rtc.util.RxScheduler;
import com.deepblue.webrtcpeer.rtc_comm.ws.BaseSocketCallback;
import com.deepblue.webrtcpeer.rtc_comm.ws.DefaultSocketService;
import com.deepblue.webrtcpeer.rtc_comm.ws.SocketService;
import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionClient;
import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionParameters;
import com.deepblue.webrtcpeer.rtc_peer.SignalingEvents;
import com.deepblue.webrtcpeer.rtc_peer.SignalingParameters;
import com.deepblue.webrtcpeer.rtc_peer.config.DefaultConfig;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;

import java.util.LinkedList;

/**
 * Created by nhancao on 7/20/17.
 */

public class One2OnePresenter extends MvpBasePresenter<One2OneView>
        implements SignalingEvents, PeerConnectionClient.PeerConnectionEvents {
    public static final String STREAM_HOST = "ws://192.168.2.101:8443/call";
    private static final String TAG = One2OnePresenter.class.getSimpleName();
    private Application application;
    private SocketService socketService;
    private Gson gson;

    private PeerConnectionClient peerConnectionClient;
    private KurentoOne2OneRTCClient rtcClient;
    private PeerConnectionParameters peerConnectionParameters;
    private DefaultConfig defaultConfig;
    private RTCAudioManager audioManager;
    private SignalingParameters signalingParameters;
    private boolean iceConnected;

    public One2OnePresenter(Application application) {
        this.application = application;
        this.socketService = new DefaultSocketService(application);
        this.gson = new Gson();
    }

    public void initPeerConfig(String fromPeer, String toPeer, boolean isHost) {
        rtcClient = new KurentoOne2OneRTCClient(socketService, fromPeer, toPeer, isHost);
        defaultConfig = new DefaultConfig();
        peerConnectionParameters = defaultConfig.createPeerConnectionParams();
        peerConnectionClient = PeerConnectionClient.getInstance();
        peerConnectionClient.createPeerConnectionFactory(
                application.getApplicationContext(), peerConnectionParameters, this);
    }

    public void disconnect() {
        if (rtcClient != null) {
            rtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }

        if (socketService != null) {
            socketService.close();
        }

        if (isViewAttached()) {
            getView().disconnect();
        }
    }


    public void connectServer() {

        socketService.connect(One2OnePresenter.STREAM_HOST, new BaseSocketCallback() {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                super.onOpen(serverHandshake);
                RxScheduler.runOnUi(o -> {
                    getView().logAndToast("Connected");
                    getView().socketConnect(true);
                });
            }

            @Override
            public void onMessage(String serverResponse_) {
                super.onMessage(serverResponse_);
                try {
                    ServerResponse serverResponse = gson.fromJson(serverResponse_, ServerResponse.class);

                    switch (serverResponse.getIdRes()) {
                        case REGISTER_RESPONSE:
                            if (serverResponse.getTypeRes() == TypeResponse.REJECTED) {
                                RxScheduler.runOnUi(o -> {
                                    if (isViewAttached()) {
                                        getView().logAndToast(serverResponse.getMessage());
                                        getView().registerStatus(false);
                                    }
                                });
                            } else {
                                RxScheduler.runOnUi(o -> {
                                    if (isViewAttached()) {
                                        getView().registerStatus(true);
                                    }
                                });
                            }
                            break;
                        case INCOMING_CALL:
                            RxScheduler.runOnUi(o -> {
                                if (isViewAttached()) {
                                    getView().incomingCalling(serverResponse.getFrom());
                                }
                            });
                            break;
                        case CALL_RESPONSE:
                            if (serverResponse.getTypeRes() == TypeResponse.REJECTED) {
                                RxScheduler.runOnUi(o -> {
                                    if (isViewAttached()) {
                                        getView().logAndToast(serverResponse.getMessage());
                                    }
                                });
                            } else {
                                SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,
                                                                                serverResponse.getSdpAnswer());
                                onRemoteDescription(sdp);
                                RxScheduler.runOnUi(o -> {
                                    if (isViewAttached()) {
                                        getView().startCallIng();
                                    }
                                });
                            }

                            break;

                        case ICE_CANDIDATE:
                            CandidateModel candidateModel = serverResponse.getCandidate();
                            onRemoteIceCandidate(
                                    new IceCandidate(candidateModel.getSdpMid(), candidateModel.getSdpMLineIndex(),
                                                     candidateModel.getSdp()));
                            break;

                        case START_COMMUNICATION:
                            SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,
                                                                            serverResponse.getSdpAnswer());
                            onRemoteDescription(sdp);
                            RxScheduler.runOnUi(o -> {
                                if (isViewAttached()) {
                                    getView().startCallIng();
                                }
                            });
                            break;

                        case STOP_COMMUNICATION:
                            RxScheduler.runOnUi(o -> {
                                if (isViewAttached()) {
                                    getView().stopCalling();
                                }
                            });
                            break;
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                super.onClose(i, s, b);
                RxScheduler.runOnUi(o -> {
                    getView().logAndToast("Closed");
                    getView().socketConnect(false);
                    disconnect();
                });
            }

            @Override
            public void onError(Exception e) {
                super.onError(e);
                RxScheduler.runOnUi(o -> {
                    getView().socketConnect(false);
                    disconnect();
                });
            }
        });

    }

    public void register(String name) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "register");
            obj.put("name", name);

            socketService.sendMessage(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "register failed");
        }
        Log.i(TAG, "register success");

    }

    public void startCall() {
        if (rtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }

        SignalingParameters parameters = new SignalingParameters(
                new LinkedList<PeerConnection.IceServer>() {
                    {
                        add(new PeerConnection.IceServer("stun:139.196.37.113:3478", "dragonchang","123456"));
                    }
                }, true, null, null, null, null, null);
        onSignalConnected(parameters);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = RTCAudioManager.create(application.getApplicationContext());
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start((audioDevice, availableAudioDevices) ->
                                   Log.d(TAG, "onAudioManagerDevicesChanged: " + availableAudioDevices + ", "
                                              + "selected: " + audioDevice));
    }

    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    private void callConnected() {
        if (peerConnectionClient == null) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, 1000);
        getView().setSwappedFeeds(false);
    }

    @Override
    public void onSignalConnected(SignalingParameters params) {
        Log.e(TAG, "SignalingEvents:: onSignalConnected.");
        RxScheduler.runOnUi(o -> {
            if (isViewAttached()) {
                signalingParameters = params;
                VideoCapturer videoCapturer = null;
                if (peerConnectionParameters.videoCallEnabled) {
                    videoCapturer = getView().createVideoCapturer();
                }
                peerConnectionClient
                        .createPeerConnection(getView().getEglBaseContext(), getView().getLocalProxyRenderer(),
                                              getView().getRemoteProxyRenderer(), videoCapturer,
                                              signalingParameters);

                if (isViewAttached()) getView().logAndToast("Creating OFFER...");
                peerConnectionClient.createOffer();
            }
        });
    }

    @Override
    public void onRemoteDescription(SessionDescription sdp) {
        Log.e(TAG, "SignalingEvents:: onRemoteDescription.");
        RxScheduler.runOnUi(o -> {
            if (peerConnectionClient == null) {
                Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                return;
            }
            peerConnectionClient.setRemoteDescription(sdp);
            if (!signalingParameters.initiator) {
                if (isViewAttached()) getView().logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(IceCandidate candidate) {
        Log.e(TAG, "SignalingEvents:: onRemoteIceCandidate.");
        RxScheduler.runOnUi(o -> {
            if (peerConnectionClient == null) {
                Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                return;
            }
            peerConnectionClient.addRemoteIceCandidate(candidate);
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.e(TAG, "SignalingEvents:: onRemoteIceCandidatesRemoved.");
        RxScheduler.runOnUi(o -> {
            if (peerConnectionClient == null) {
                Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                return;
            }
            peerConnectionClient.removeRemoteIceCandidates(candidates);
        });
    }

    @Override
    public void onChannelClose() {
        Log.e(TAG, "SignalingEvents:: onChannelClose.");
        RxScheduler.runOnUi(o -> {
            if (isViewAttached()) getView().logAndToast("Remote end hung up; dropping PeerConnection");
            disconnect();
        });
    }

    @Override
    public void onChannelError(String description) {
        Log.e(TAG, "SignalingEvents:: onChannelError.");
        Log.e(TAG, "onChannelError: " + description);
    }





    @Override
    public void onLocalDescription(SessionDescription sdp) {
        Log.e(TAG, "PeerConnectionEvents:: onLocalDescription.");
        RxScheduler.runOnUi(o -> {
            if (rtcClient != null) {
                if (signalingParameters.initiator) {
                    rtcClient.sendOfferSdp(sdp);
                } else {
                    rtcClient.sendAnswerSdp(sdp);
                }
            }
            if (peerConnectionParameters.videoMaxBitrate > 0) {
                Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
            }
        });
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.e(TAG, "PeerConnectionEvents:: onIceCandidate.");
        RxScheduler.runOnUi(o -> {
            if (rtcClient != null) {
                rtcClient.sendLocalIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.e(TAG, "PeerConnectionEvents:: onIceCandidatesRemoved.");
        RxScheduler.runOnUi(o -> {
            if (rtcClient != null) {
                rtcClient.sendLocalIceCandidateRemovals(candidates);
            }
        });
    }

    @Override
    public void onIceConnected() {
        Log.e(TAG, "PeerConnectionEvents:: onIceConnected.");
        RxScheduler.runOnUi(o -> {
            iceConnected = true;
            callConnected();
        });
    }

    @Override
    public void onIceDisconnected() {
        Log.e(TAG, "PeerConnectionEvents:: onIceDisconnected.");
        RxScheduler.runOnUi(o -> {
            if (isViewAttached()) getView().logAndToast("ICE disconnected");
            iceConnected = false;
            disconnect();
        });
    }

    @Override
    public void onPeerConnectionClosed() {
        Log.e(TAG, "PeerConnectionEvents:: onPeerConnectionClosed.");

    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
        Log.e(TAG, "PeerConnectionEvents:: onPeerConnectionStatsReady.");
        RxScheduler.runOnUi(o -> {
            if (iceConnected) {
                Log.e(TAG, "run: " + reports);
            }
        });
    }

    @Override
    public void onPeerConnectionError(String description) {
        Log.e(TAG, "PeerConnectionEvents:: onPeerConnectionError.");
        Log.e(TAG, "onPeerConnectionError: " + description);
    }

}
