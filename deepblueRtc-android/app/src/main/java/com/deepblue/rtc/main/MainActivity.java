package com.deepblue.rtc.main;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.deepblue.rtccall.bean.UserBean;
import com.deepblue.rtccall.ims.DeepBlueVideoCallManger;
import com.deepblue.rtccall.ims.ImServerMessageCallBack;
import com.deepblue.rtccall.ims.response.ResponseType;
import com.deepblue.rtccall.ims.response.ServerResponse;
import com.deepblue.rtccall.ui.ChatSingleActivity;
import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.deepblue.rtc.R;
import com.nhancv.npermission.NPermission;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

/**
 * Created by nhancao on 9/18/16.
 */

@EActivity(R.layout.activity_main)
public class MainActivity extends MvpActivity<MainView, MainPresenter> implements MainView,
        NPermission.OnPermissionResult, ImServerMessageCallBack {
    private static final String TAG = MainActivity.class.getName();
    private NPermission nPermission;
    private boolean isCameraGranted;
    private boolean isAudioGranted;
    UserBean local = new UserBean();
    UserBean remote = new UserBean();
    @ViewById(R.id.userName)
    protected EditText userName;
    @ViewById(R.id.callUserName)
    protected EditText callUserName;

    @ViewById(R.id.register)
    protected Button register;
    @ViewById(R.id.call)
    protected Button call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DeepBlueVideoCallManger.getInstance(this.getApplication());
        DeepBlueVideoCallManger.getInstance(this.getApplication()).registerImsMessageCallBack(this);
    }

    @AfterViews
    protected void init() {
        register.setEnabled(true);
        call.setEnabled(false);
    }

    @Click(R.id.register)
    protected void register() {
        if(!TextUtils.isEmpty(userName.getText().toString())) {
            local.setName(userName.getText().toString());
            DeepBlueVideoCallManger.getInstance(this.getApplication()).registerUser(local);
        } else {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
        }
    }

    @Click(R.id.call)
    protected void callClick() {
        if(!TextUtils.isEmpty(userName.getText().toString())
                && !TextUtils.isEmpty(callUserName.getText().toString())) {
            local.setName(userName.getText().toString());
            remote.setName(callUserName.getText().toString());
            ChatSingleActivity.openActivity(this, true, remote, local);
        } else {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    @Override
    public MainPresenter createPresenter() {
        return new MainPresenter(getApplication());
    }


    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < 23) {
        } else {
            nPermission = new NPermission(true);
            nPermission.requestPermission(this, Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        nPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionResult(String permission, boolean isGranted) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                this.isCameraGranted = isGranted;
                if (!isGranted) {
                    nPermission.requestPermission(this, Manifest.permission.CAMERA);
                } else if (!isAudioGranted) {
                    nPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);
                }
                break;
            case Manifest.permission.RECORD_AUDIO:
                this.isAudioGranted = isGranted;
                if (!isGranted) {
                    nPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);
                } else if (!isCameraGranted) {
                    nPermission.requestPermission(this, Manifest.permission.CAMERA);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void imsRegisterResponse(ServerResponse serverResponse) {
        runOnUiThread(() -> {
            if(serverResponse.getTypeRes() == ResponseType.REJECTED) {
                if(!TextUtils.isEmpty(serverResponse.getMessage()) &&
                        serverResponse.getMessage().contains("already registered")) {
                    Toast.makeText(this, "重复注册", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
            }
            register.setEnabled(false);
            call.setEnabled(true);
        });
    }

    @Override
    public void imsIncomingCall(ServerResponse serverResponse) {

    }

    @Override
    public void imsCallResponse(ServerResponse serverResponse) {

    }

    @Override
    public void imsIceCandidate(ServerResponse serverResponse) {

    }

    @Override
    public void imsStartCommunication(ServerResponse serverResponse) {

    }

    @Override
    public void imsStopCommunication(ServerResponse serverResponse) {

    }
}
