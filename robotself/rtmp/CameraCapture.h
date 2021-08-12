#ifndef H_CAMERA_CAPTURE
#define H_CAMERA_CAPTURE
#include <unistd.h>
#include <iostream>
#include <sys/time.h>

#include "../handlerThread/Handler.h"
#include "../handlerThread/Message.h"
#include "../handlerThread/Looper.h"
#include "../handlerThread/NThread.h"

#include <opencv4/opencv2/highgui.hpp>
#include <opencv4/opencv2/opencv.hpp>

using namespace std;
using namespace cv;
class CameraCaptureHandler;
class CameraCapture
{
public:
	CameraCapture();
	virtual ~CameraCapture();

public:
	void startCameraCapture();
	void doCapture();
	long getFramerate() { return mFramerate; }

private:
	string gstreamerPipeline();
	bool openCamera(int capture_width = 1280, int capture_height = 720, int display_width = 1280, int display_height = 720, int framerate = 30, int flip_method = 0);
	bool isCISCamera();
private:
	Looper* mMainLooper;
	CameraCaptureHandler *mCaptureHandler;
	NThread *mCapturethread;

	VideoCapture *mVideoCapture;

	//是否打开摄像头
	bool mHasOpenCamera;

	//打开摄像参数
	int mCaptureWidth;
	int mCaptureHeight;
	int mDisplayWidth;
	int mDisplayHeight;
	int mFramerate;
	int mFlipMethod;

	//默认使用/dev/video0
	int mCameraDeviceIndex;

};

class CameraCaptureHandler : public Handler {

public:
	static const int CAPTURE_MESSAGE = 1;

private:
	CameraCapture *mCameraCapture;

public:
	CameraCaptureHandler(Looper* looper, CameraCapture *cameraCapture);

private:
	void handlerMessage(Message *message);
	long getNextFrameDelay(timeval startTime, timeval endTime);
};
#endif
