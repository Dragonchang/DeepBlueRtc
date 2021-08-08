package com.deepblue.rtccall.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.deepblue.rtccall.R;


/**
 * 单聊控制界面
 * Created by dds on 2019/1/7.
 * android_shuai@163.com
 */
public class ChatSingleFragment extends Fragment {

    private View rootView;

    //拨出和接听显示信息控制
    LinearLayout mInviteeInfoContainer;

    //拨出电话界面
    private LinearLayout mOutgoingView;

    //来电通话界面
    private LinearLayout mIncomingView;
    //挂断电话button
    private ImageView incomingHangupImageView;
    //接听通话按钮
    private ImageView acceptImageView;

    //通话中界面
    private LinearLayout mDialingView;

    private ChatSingleActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (ChatSingleActivity) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = onInitloadView(inflater, container, savedInstanceState);
            initView(rootView);
            initListener();
        }
        return rootView;
    }

    /**
     * 更新到拨打通话中状态
     */
    public void updateToOutgoingStatus() {
        mInviteeInfoContainer.setVisibility(View.VISIBLE);
        mOutgoingView.setVisibility(View.VISIBLE);
        mIncomingView.setVisibility(View.GONE);
        mDialingView.setVisibility(View.GONE);
    }

    /**
     * 更新到来电状态
     */
    public void updateToIncomingCallStatus() {
        mInviteeInfoContainer.setVisibility(View.VISIBLE);
        mOutgoingView.setVisibility(View.GONE);
        mIncomingView.setVisibility(View.VISIBLE);
        mDialingView.setVisibility(View.GONE);
    }

    /**
     * 更新到接听状态
     */
    public void updateToDialingStatus() {
        mInviteeInfoContainer.setVisibility(View.GONE);
        mOutgoingView.setVisibility(View.GONE);
        mIncomingView.setVisibility(View.GONE);
        mDialingView.setVisibility(View.VISIBLE);
    }

    /**
     * 更新到挂断电话状态
     */
    public void updateToHangUpStatus() {

    }

    private View onInitloadView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_control_single, container, false);
    }

    private void initView(View rootView) {
        //拨出和接听显示信息控制
        mInviteeInfoContainer = rootView.findViewById(R.id.inviteeInfoContainer);

        //拨出通话
        mOutgoingView = rootView.findViewById(R.id.outgoingActionContainer);

        //来电通话
        mIncomingView = rootView.findViewById(R.id.incomingActionContainer);
        incomingHangupImageView = rootView.findViewById(R.id.incomingHangupImageView);
        acceptImageView = rootView.findViewById(R.id.acceptImageView);

        //通话中
        mDialingView = rootView.findViewById(R.id.connectedActionContainer);
    }

    private void initListener() {
        incomingHangupImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.hangUp();
            }
        });

        acceptImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.incomingCall();
            }
        });
    }

}
