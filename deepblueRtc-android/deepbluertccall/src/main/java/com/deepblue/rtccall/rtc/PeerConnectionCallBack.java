package com.deepblue.rtccall.rtc;

import android.util.Log;

import com.deepblue.webrtcpeer.rtc_peer.config.DefaultConfig;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

public interface PeerConnectionCallBack {

    void onLocalDescription(SessionDescription sdp);

    void onIceCandidate(IceCandidate candidate);

    void onIceCandidatesRemoved(IceCandidate[] candidates);

    void onIceConnected();

    void onIceDisconnected();

    void onPeerConnectionClosed();

    void onPeerConnectionStatsReady(StatsReport[] reports);

    void onPeerConnectionError(String description);

}
