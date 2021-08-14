#include"CameraCapture.h"

/*****************************************************************
*
* function��CameraCapture
* ���ã����캯��
*
*****************************************************************/
CameraCapture::CameraCapture()
{
	mCapturethread = new NThread();
	mCaptureHandler = new CameraCaptureHandler(mCapturethread->getLooper(), this);
	mHasOpenCamera = false;
	mCameraDeviceIndex = 1;
	mCaptureWidth = 346;
	mCaptureHeight = 260;
	mDisplayWidth = 346;
	mDisplayHeight = 260;
	mFramerate = 25;
	mFlipMethod = 0;
	mPushRtmp = new PushRtmp(mDisplayWidth, mDisplayHeight, mFramerate, mCameraDeviceIndex);
}

/*****************************************************************
*
* function��~CameraCapture
* ���ã���������
*
*****************************************************************/
CameraCapture::~CameraCapture()
{
	if (mVideoCapture != NULL) {
		if (mVideoCapture->isOpened())
		{
			mVideoCapture->release();
		}
		delete mVideoCapture;
		mVideoCapture = NULL;
	}
}

/*****************************************************************
*
* function��startCameraCapture
* ����doCapture����ʼץȡ����ͷ�ź�
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
* function��openCamera
* ���ã�
*
*****************************************************************/
bool CameraCapture::openCamera()
{
	try
	{
		if (isCISCamera())
		{
			printf("openCamera index 0\n");
			string pipeline = gstreamerPipeline();
			mVideoCapture = new VideoCapture(pipeline, CAP_GSTREAMER);
		}
		else 
		{
			printf("openCamera index 1\n");
			mVideoCapture = new VideoCapture("/home/deepblue/zfl/test.flv");
			printf("openCamera index 111111111111\n");
			/*
			mVideoCapture->open("/home/deepblue/zfl/test.flv");
			printf("openCamera index 2222\n");
			mVideoCapture->set(cv::CAP_PROP_FRAME_WIDTH, mCaptureWidth);
			printf("openCamera index 3333\n");
			mVideoCapture->set(cv::CAP_PROP_FRAME_HEIGHT, mCaptureHeight);
			printf("openCamera index 444\n");
			mVideoCapture->set(cv::CAP_PROP_FPS, mFramerate);
			printf("openCamera index 55555\n");
			*/
		}
		if (isCameraOpen())
		{
			printf("openCamera index 666666666\n");
			mHasOpenCamera = true;
			int inWidth = mVideoCapture->get(cv::CAP_PROP_FRAME_WIDTH);
			int inHeight = mVideoCapture->get(cv::CAP_PROP_FRAME_HEIGHT);
			int fps = mVideoCapture->get(cv::CAP_PROP_FPS);
			int nframes = mVideoCapture->get(cv::CAP_PROP_FRAME_COUNT);
			printf("openCamera success inWidth: %d, inHeight: %d,fps:%d, nframes:%d\n", inWidth, inHeight, fps, nframes);
			return true;
		}
		else {
			printf("openCamera index 777777777777777777\n");
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
* function:�Ƿ��ǰ�������ͷ
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
* function:����ͷ�Ƿ��
*
*****************************************************************/
bool CameraCapture::isCameraOpen()
{
	printf("isCameraOpen index 11111111\n");
	if (mVideoCapture != NULL && mVideoCapture->isOpened())
	{
		printf("isCameraOpen index 222222222\n");
		return true;
	}
	printf("isCameraOpen index 33333333\n");
	return false;
}

/*****************************************************************
* name:cvmatToAvframe
* function:Mat ת AVFrame 
*
*****************************************************************/
AVFrame* CameraCapture::cvmatToAvframe(Mat* image)
{
	AVFrame *avframe = av_frame_alloc();
	if ( avframe!= NULL && !image->empty()) {
		int width = image->cols;
		int height = image->rows;
		printf("cvmatToAvframe inWidth: %d, height: %d\n", width, height);
		avframe->format = AV_PIX_FMT_YUV420P;
		avframe->width = width;
		avframe->height = height;
		av_frame_get_buffer(avframe, 0);
		av_frame_make_writable(avframe);
		cv::Mat yuv; // convert to yuv420p first
		cv::cvtColor(*image, yuv, cv::COLOR_BGR2YUV_I420);
		// calc frame size
		int frame_size = image->cols * image->rows;
		unsigned char *pdata = yuv.data;
		avframe->data[0] = pdata; // fill y
		avframe->data[1] = pdata + frame_size; // fill u
		avframe->data[2] = pdata + frame_size * 5 / 4; // fill v
	}
	else {
		return NULL;
	}
	return avframe;
}

/*****************************************************************
* name:doCapture
* function:ʹ��opencvץȡ����ͷ�ź�
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
	mPushRtmp->pushRtmp(cvmatToAvframe(&frame));
}

/*****************************************************************
*
* function��gstreamerPipeline
* ���ã�
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
* function��CameraCaptureHandler
* ���ã���������ͷ�źŵ�handler
*
*****************************************************************/
CameraCaptureHandler::CameraCaptureHandler(Looper* looper, CameraCapture *cameraCapture) :Handler(looper)
{
	mCameraCapture = cameraCapture;
}

/*****************************************************************
*
* function��handlerMessage
* ���ã���������ͷ�źŵĺ���
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
* function:ʹ��opencvץȡ����ͷ�ź�
* parm:costTime ��ȡ��һ֡����ʱ��
*****************************************************************/
long CameraCaptureHandler::getNextFrameDelay(timeval startTime, timeval endTime)
{
	long temp = (endTime.tv_sec - startTime.tv_sec) * 1000 + (endTime.tv_usec - startTime.tv_usec)/ 1000;
	printf("doCapture cost time = %ld\n", temp);
	return 1000 / mCameraCapture->getFramerate() - temp;
}
