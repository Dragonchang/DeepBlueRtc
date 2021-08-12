#include"CameraCapture.h"

CameraCapture::CameraCapture()
{
	mCapturethread = new NThread();
	//使用主线程作为采集摄像头信号的线程
	mCaptureHandler = new CameraCaptureHandler(mCapturethread->getLooper(), this);
	mHasOpenCamera = false;
	mCameraDeviceIndex = 1;
}

CameraCapture::~CameraCapture()
{

}

/*****************************************************************
*
* function：startCameraCapture
* 作用doCapture：开始抓取摄像头信号
*
*****************************************************************/
void CameraCapture::startCameraCapture()
{
	printf("openCamera begin!*************************\n");
	bool isSuccess = openCamera();
	if (!isSuccess)
	{
		printf("openCamera failed!");
	}
	printf("openCamera end!*************************\n");
	Message* message = Message::obtain(CameraCaptureHandler::CAPTURE_MESSAGE);
	mCaptureHandler->sendMessage(message);
}



/*****************************************************************
*
* function：openCamera
* 作用：
*
*****************************************************************/
bool CameraCapture::openCamera(int capture_width, int capture_height, int display_width, int display_height, int framerate, int flip_method)
{
	try
	{
		mCaptureWidth = capture_width;
		mCaptureHeight = capture_height;
		mDisplayWidth = display_width;
		mDisplayHeight = display_height;
		mFramerate = framerate;
		mFlipMethod = flip_method;
		if (isCISCamera())
		{
			printf("openCamera index 0\n");
			string pipeline = gstreamerPipeline();
			mVideoCapture = new VideoCapture(pipeline, CAP_GSTREAMER);
		}
		else 
		{
			printf("openCamera index 1\n");
			mVideoCapture = new VideoCapture(1);
			mVideoCapture->set(cv::CAP_PROP_FRAME_WIDTH, 640);
			mVideoCapture->set(cv::CAP_PROP_FRAME_HEIGHT, 480);
			mVideoCapture->set(cv::CAP_PROP_FPS, 30);
		}
		if (isCameraOpen())
		{
			mHasOpenCamera = true;
			int inWidth = mVideoCapture->get(cv::CAP_PROP_FRAME_WIDTH);
			int inHeight = mVideoCapture->get(cv::CAP_PROP_FRAME_HEIGHT);
			int fps = mVideoCapture->get(cv::CAP_PROP_FPS);
			int nframes = mVideoCapture->get(cv::CAP_PROP_FRAME_COUNT);
			printf("openCamera success inWidth: %d, inHeight: %d,fps:%d, nframes:%d\n", inWidth, inHeight, fps, nframes);
			return true;
		}
		else {
			mHasOpenCamera = false;
			delete mVideoCapture;
			mVideoCapture = NULL;
		}
	}
	catch (exception &ex)
	{
		if (mVideoCapture->isOpened()) {
			mVideoCapture->release();
			delete mVideoCapture;
			mVideoCapture = NULL;
		}
			
	}
	return false;
}

/*****************************************************************
* name:isCISCamera
* function:是否是板载摄像头
*
*****************************************************************/
bool CameraCapture::isCISCamera()
{
	if (mCameraDeviceIndex == 0)
	{
		return true;
	}
	return false;
}


/*****************************************************************
* name:isCameraOpen
* function:摄像头是否打开
*
*****************************************************************/
bool CameraCapture::isCameraOpen()
{
	if (mVideoCapture != NULL && mVideoCapture->isOpened())
	{
		return true;
	}
	return false;
}

/*****************************************************************
* name:doCapture
* function:使用opencv抓取摄像头信号
*
*****************************************************************/
void CameraCapture::doCapture()
{
	if (!isCameraOpen())
	{
		printf("doCapture with close camera\n");
		bool isSuccess = openCamera();
		if (!isSuccess)
		{
			printf("openCamera failed!\n");
			//sleep for reduce CPU time 
			sleep(1);
			return;
		}
	}
	Mat frame;
	if (!mVideoCapture->grab())
	{
		printf("Grabs the next frame from capturing device is empty\n");
		return;
	}
	if (!mVideoCapture->retrieve(frame))
	{
		printf("Decodes and returns the grabbed video frame failed\n");
		return;
	}
	printf("doCapture sucess!\n");
}

/*****************************************************************
*
* function：gstreamerPipeline
* 作用：
*
*****************************************************************/
string CameraCapture::gstreamerPipeline()
{
	return "nvarguscamerasrc ! video/x-raw(memory:NVMM),sensor-id=(int)" + std::to_string(mCameraDeviceIndex)+ ", width=(int)" + std::to_string(mCaptureWidth) + ", height=(int)" +
		std::to_string(mCaptureHeight) + ", format=(string)NV12, framerate=(fraction)" + std::to_string(mFramerate) +
		"/1 ! nvvidconv flip-method=" + std::to_string(mFlipMethod) + " ! video/x-raw, width=(int)" + std::to_string(mDisplayWidth) + ", height=(int)" +
		std::to_string(mDisplayHeight) + ", format=(string)BGRx ! videoconvert ! video/x-raw, format=(string)BGR ! appsink";
}

/*****************************************************************
*
* function：CameraCaptureHandler
* 作用：处理摄像头信号的handler
*
*****************************************************************/
CameraCaptureHandler::CameraCaptureHandler(Looper* looper, CameraCapture *cameraCapture) :Handler(looper)
{
	mCameraCapture = cameraCapture;
}

/*****************************************************************
*
* function：handlerMessage
* 作用：处理摄像头信号的函数
*
*****************************************************************/
void CameraCaptureHandler::handlerMessage(Message *message)
{
	timeval startTime;
	timeval endTime;
	gettimeofday(&startTime, nullptr);
	printf("begin doCapture*********************\n");
	mCameraCapture->doCapture();
	gettimeofday(&endTime, nullptr);
	long delay = getNextFrameDelay(startTime, endTime);
	printf("doCapture end need delay: %ld\n", delay);
	Message* newMessage = Message::obtain(CameraCaptureHandler::CAPTURE_MESSAGE);
	if (delay >0) {
		sendMessageDelayed(newMessage, delay);
	}
	else
	{
		sendMessage(newMessage);
	}
}

/*****************************************************************
* name:getNextFrameDelay
* function:使用opencv抓取摄像头信号
* parm:costTime 获取上一帧花费时间
*****************************************************************/
long CameraCaptureHandler::getNextFrameDelay(timeval startTime, timeval endTime)
{
	long temp = (endTime.tv_sec - startTime.tv_sec) * 1000 + (endTime.tv_usec - startTime.tv_usec)/ 1000;
	printf("doCapture cost time = %ld\n", temp);
	return 1000 / mCameraCapture->getFramerate() - temp;
}
