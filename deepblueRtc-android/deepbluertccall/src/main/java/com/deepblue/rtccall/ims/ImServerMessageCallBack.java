package com.deepblue.rtccall.ims;

import com.deepblue.rtccall.ims.response.ServerResponse;

public interface ImServerMessageCallBack {
    void imsRegisterResponse(ServerResponse serverResponse);

    void imsIncomingCall(ServerResponse serverResponse);

    void imsCallResponse(ServerResponse serverResponse);

    void imsIceCandidate(ServerResponse serverResponse);

    void imsStartCommunication(ServerResponse serverResponse);

    void imsStopCommunication(ServerResponse serverResponse);
}
