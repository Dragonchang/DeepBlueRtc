package com.deepblue.rtccall.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.deepblue.rtccall.R;


/**
 * 单聊控制界面
 * Created by dds on 2019/1/7.
 * android_shuai@163.com
 */
public class ChatSingleFragment extends Fragment {

    public View rootView;
    private TextView wr_switch_hang_up;
    private boolean enableMic = true;
    private boolean enableSpeaker = false;
    private boolean videoEnable;
    private ChatSingleActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (ChatSingleActivity) getActivity();
        Bundle bundle = getArguments();
        if (bundle != null) {
            videoEnable = bundle.getBoolean("videoEnable");
        }
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

    private View onInitloadView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_control_single, container, false);
    }

    private void initView(View rootView) {
        //wr_switch_hang_up = rootView.findViewById(R.id.wr_switch_hang_up);
    }

    private void initListener() {
//        wr_switch_hang_up.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                activity.hangUp();
//            }
//        });
    }

}
