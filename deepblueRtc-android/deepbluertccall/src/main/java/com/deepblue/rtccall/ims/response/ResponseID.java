package com.deepblue.rtccall.ims.response;

/**
 * IM server ws message type
 */
public enum ResponseID {
    /**
     * 用户上线注册消息应答
     */
    REGISTER_RESPONSE("registerResponse"),
    PRESENTER_RESPONSE("presenterResponse"),

    /**
     *
     */
    ICE_CANDIDATE("iceCandidate"),
    VIEWER_RESPONSE("viewerResponse"),
    STOP_COMMUNICATION("stopCommunication"),
    CLOSE_ROOM_RESPONSE("closeRoomResponse"),
    INCOMING_CALL("incomingCall"),
    START_COMMUNICATION("startCommunication"),
    CALL_RESPONSE("callResponse"),
    UN_KNOWN("unknown");

    private String id;

    ResponseID(String id) {
        this.id = id;
    }

    public static ResponseID getIdRes(String idRes) {
        for (ResponseID idResponse : ResponseID.values()) {
            if (idRes.equals(idResponse.getId())) {
                return idResponse;
            }
        }
        return UN_KNOWN;
    }

    public String getId() {
        return id;
    }
}
