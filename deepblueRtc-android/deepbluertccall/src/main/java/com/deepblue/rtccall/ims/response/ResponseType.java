package com.deepblue.rtccall.ims.response;

/**
 * im server 应答类型
 */
public enum ResponseType {
    ACCEPTED("accepted"),
    REJECTED("rejected");

    private String id;

    ResponseType(String id) {
        this.id = id;
    }

    public static ResponseType getType(String type) {
        for (ResponseType typeResponse : ResponseType.values()) {
            if (type.equals(typeResponse.getId())) {
                return typeResponse;
            }
        }
        return REJECTED;
    }

    public String getId() {
        return id;
    }
}
