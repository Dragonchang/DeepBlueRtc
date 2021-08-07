package com.deepblue.rtccall.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.deepblue.rtccall.rtc.RTCEngineConfig;
import com.deepblue.rtccall.R;
import com.deepblue.rtccall.bean.UserBean;
import com.deepblue.rtccall.ims.DeepBlueVideoCallManger;
import com.deepblue.webrtcpeer.rtc_plugins.ProxyRenderer;
import com.nhancv.npermission.NPermission;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

/**
 * one2one 视频语音聊天
 */
public class ChatSingleActivity extends AppCompatActivity implements NPermission.OnPermissionResult, ViewEntity{

    private ChatSingleFragment mChatSingleFragment;
    protected SurfaceViewRenderer vGLSurfaceViewCallFull;
    protected SurfaceViewRenderer vGLSurfaceViewCallPip;
    private ProxyRenderer localProxyRenderer;
    private ProxyRenderer remoteProxyRenderer;


    private EglBase rootEglBase;

    //判断是否拨出通话
    private boolean isOutgoing = false;

    //拨打通话对象和来电的对象
    private UserBean remoteUserBean;

    //本地用户
    private UserBean localUserBean;

    private boolean isSwappedFeeds;

    private NPermission mNPermission;
    private boolean isCameraGranted;
    private boolean isAudioGranted;

    public static void openActivity(Context context, boolean isOutgoing,
                                    UserBean remoteUserBean, UserBean localUserBean) {
        Intent intent = new Intent(context, ChatSingleActivity.class);
        intent.putExtra("isOutgoing", isOutgoing);
        intent.putExtra("remoteUserBean", remoteUserBean);
        intent.putExtra("localUserBean", localUserBean);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_single);
        initVar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isOutgoing) {
            updateToOutgoingStatus();
        } else {
            updateToIncomingCallStatus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
    private void initVar() {
        mNPermission = new NPermission(true);
        Intent intent = getIntent();
        isOutgoing = intent.getBooleanExtra("isOutgoing", false);
        remoteUserBean = (UserBean)intent.getSerializableExtra("remoteUserBean");
        localUserBean = (UserBean)intent.getSerializableExtra("localUserBean");
        mChatSingleFragment = new ChatSingleFragment();
        replaceFragment(mChatSingleFragment);


        //config peer
        localProxyRenderer = new ProxyRenderer();
        remoteProxyRenderer = new ProxyRenderer();
        rootEglBase = EglBase.create();

        vGLSurfaceViewCallFull = this.findViewById(R.id.remote_view_render);
        vGLSurfaceViewCallFull.init(rootEglBase.getEglBaseContext(), null);
        vGLSurfaceViewCallFull.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        vGLSurfaceViewCallFull.setEnableHardwareScaler(true);
        vGLSurfaceViewCallFull.setMirror(true);

        vGLSurfaceViewCallPip = this.findViewById(R.id.local_view_render);
        vGLSurfaceViewCallPip.init(rootEglBase.getEglBaseContext(), null);
        vGLSurfaceViewCallPip.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        vGLSurfaceViewCallPip.setEnableHardwareScaler(true);
        vGLSurfaceViewCallPip.setMirror(true);
        vGLSurfaceViewCallPip.setZOrderMediaOverlay(true);

        // Swap feeds on pip view click.
        vGLSurfaceViewCallPip.setOnClickListener(view -> setSwappedFeeds(!isSwappedFeeds));
        setSwappedFeeds(true);

        RTCEngineConfig rtcEngineConfig = new RTCEngineConfig();
        rtcEngineConfig.isOutgoing = isOutgoing;
        rtcEngineConfig.localUserBean = localUserBean;
        rtcEngineConfig.remoteUserBean = remoteUserBean;
        DeepBlueVideoCallManger.getInstance(this.getApplication()).createRTCEngine(this, rtcEngineConfig);
        if(isOutgoing) {
            startCall();
        }
    }


    @Override
    public void setSwappedFeeds(boolean isSwappedFeeds) {
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyRenderer.setTarget(isSwappedFeeds ? vGLSurfaceViewCallPip : vGLSurfaceViewCallFull);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? vGLSurfaceViewCallFull : vGLSurfaceViewCallPip);
        vGLSurfaceViewCallFull.setMirror(isSwappedFeeds);
        vGLSurfaceViewCallPip.setMirror(!isSwappedFeeds);
    }

    //拨打电话
    public void startCall() {
        if (Build.VERSION.SDK_INT < 23) {
            DeepBlueVideoCallManger.getInstance(this.getApplication()).StartCalling(localUserBean, remoteUserBean);
        } else {
            mNPermission.requestPermission(this, Manifest.permission.CAMERA);
        }
    }

    //接听电话
    public void incomingCall() {
        if (Build.VERSION.SDK_INT < 23) {
            //接听电话

        } else {
            mNPermission.requestPermission(this, Manifest.permission.CAMERA);
        }
    }

    // 挂断
    public void hangUp() {
        //disConnect();
        this.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mNPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionResult(String permission, boolean isGranted) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                this.isCameraGranted = isGranted;
                if (!isGranted) {
                    mNPermission.requestPermission(this, Manifest.permission.CAMERA);
                } else if(!isAudioGranted) {
                    mNPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);
                } else {
                    DeepBlueVideoCallManger.getInstance(this.getApplication()).StartCalling(localUserBean, remoteUserBean);
                }
                break;
            case Manifest.permission.RECORD_AUDIO:
                this.isAudioGranted = isGranted;
                if (!isGranted) {
                    mNPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);
                } else if(!isCameraGranted) {
                    mNPermission.requestPermission(this, Manifest.permission.CAMERA);
                } else {
                    DeepBlueVideoCallManger.getInstance(this.getApplication()).StartCalling(localUserBean, remoteUserBean);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            if (!captureToTexture()) {
                return null;
            }
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            return null;
        }
        return videoCapturer;
    }

    @Override
    public EglBase.Context getEglBaseContext() {
        return rootEglBase.getEglBaseContext();
    }

    @Override
    public VideoRenderer.Callbacks getLocalProxyRenderer() {
        return localProxyRenderer;
    }

    @Override
    public VideoRenderer.Callbacks getRemoteProxyRenderer() {
        return remoteProxyRenderer;
    }

    /*************************************通话状态来更新界面************************************/
    @Override
    public void updateToOutgoingStatus() {
        runOnUiThread(() -> {
                if (mChatSingleFragment != null) {
                    mChatSingleFragment.updateToOutgoingStatus();
                }
            });
    }

    @Override
    public void updateToIncomingCallStatus() {
        runOnUiThread(() -> {
            if(mChatSingleFragment != null) {
                mChatSingleFragment.updateToIncomingCallStatus();
            }
        });
    }

    @Override
    public void updateToDialingStatus() {
        runOnUiThread(() -> {
            if(mChatSingleFragment != null) {
                mChatSingleFragment.updateToDialingStatus();
            }
        });
    }

    @Override
    public void updateToHangUpStatus() {
        runOnUiThread(() -> {
            if(mChatSingleFragment != null) {
                mChatSingleFragment.updateToHangUpStatus();
            }
        });
    }


    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this) && DeepBlueVideoCallManger.getInstance(this.getApplication()).getDefaultConfig().isUseCamera2();
    }

    private boolean captureToTexture() {
        return DeepBlueVideoCallManger.getInstance(this.getApplication()).getDefaultConfig().isCaptureToTexture();
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private void replaceFragment(Fragment fragment) {
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.wr_container, fragment)
                .commit();

    }
}
