package com.deepblue.rtccall.rtc;

import com.deepblue.rtccall.bean.UserBean;

public class RTCEngineConfig {

    //判断是否拨出通话
    public boolean isOutgoing = false;

    public UserBean localUserBean;
    public UserBean remoteUserBean;
}
