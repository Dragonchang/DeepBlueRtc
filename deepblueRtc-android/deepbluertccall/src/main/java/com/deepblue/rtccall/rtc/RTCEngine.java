package com.deepblue.rtccall.rtc;


import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionClient;
import com.deepblue.webrtcpeer.rtc_peer.PeerConnectionParameters;

public interface RTCEngine {

    void createPeerConnectionFactory(final PeerConnectionParameters peerConnectionParameters);
    void startCall();

    void inComingCall();
    void disconnect();
}
