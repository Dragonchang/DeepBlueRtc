package com.deepblue.rtccall.ims.response;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * IM server response
 */

public class ServerResponse extends BaseResponse implements Serializable {
    @SerializedName("response")
    private String response;
    @SerializedName("sdpAnswer")
    private String sdpAnswer;
    @SerializedName("candidate")
    private CandidateModel candidate;
    @SerializedName("message")
    private String message;
    @SerializedName("success")
    private boolean success;
    @SerializedName("from")
    private String from;
    @SerializedName("isConnect")
    private boolean isConnect;


    public ResponseID getIdRes() {
        return ResponseID.getIdRes(getId());
    }

    public ResponseType getTypeRes() {
        return ResponseType.getType(getResponse());
    }

    public String getResponse() {
        return response;
    }

    public String getSdpAnswer() {
        return sdpAnswer;
    }

    public CandidateModel getCandidate() {
        return candidate;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFrom() {
        return from;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
