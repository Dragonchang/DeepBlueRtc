package com.deepblue.rtc.main;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.EditText;

import com.deepblue.rtc.one2one.One2OneActivity_;
import com.deepblue.rtccall.bean.UserBean;
import com.deepblue.rtccall.ims.DeepBlueVideoCallManger;
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
public class MainActivity extends MvpActivity<MainView, MainPresenter> implements MainView, NPermission.OnPermissionResult{
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

    @Click(R.id.register)
    protected void register() {
        local.setName(userName.getText().toString());
        DeepBlueVideoCallManger.getInstance(this.getApplication()).registerUser(local);
    }
    @Click(R.id.call)
    protected void callClick() {
        local.setName(userName.getText().toString());
        remote.setName(callUserName.getText().toString());
        ChatSingleActivity.openActivity(this, true, remote, local);
    }



    @Click(R.id.btOne2One)
    protected void btOne2OneClick() {
        One2OneActivity_.intent(this).start();
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
                } else if(!isAudioGranted) {
                    nPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);
                }
                break;
            case Manifest.permission.RECORD_AUDIO:
                this.isAudioGranted = isGranted;
                if (!isGranted) {
                    nPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);
                } else if(!isCameraGranted) {
                    nPermission.requestPermission(this, Manifest.permission.CAMERA);
                }
                break;
            default:
                break;
        }
    }

}
