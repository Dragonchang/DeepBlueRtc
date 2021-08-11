#include "./handlerThread/Handler.h"
#include "./handlerThread/Message.h"
#include "./handlerThread/NThread.h"
#include "./handlerThread/Condition.h"
#include "./handlerThread/Meutex.h"
#include <sys/time.h>
#include <unistd.h>
int mCount = 10;
Mutex mLock1;
Condition mCondition1;
long t(){
    struct timeval tv;
    gettimeofday(&tv,NULL);
    return tv.tv_sec*1000 + tv.tv_usec/100;
}
/*****************
1.线程的退出:子线程处理完消息退出,其它线程命令其退出.
2.looper在子线程中的使用
  創建NThread對象.
  使用該對象中的Looper 來創建處理消息的handler
3.looper在主线程中的使用
   new 一個Looper對象
   使用該looper對象創建一個處理消息的handler
   調用loop() 進入循環
4.将looper对象保存在TLS中,这样可以在子线程中handlerMessage中创建的对象中创建hanlder进行消息的处理
************************/
class TestHandler: public Handler {

    public:
    TestHandler(Looper* looper)
    :Handler(looper){

    }
    void handlerMessage(Message *message) {
        printf("tid:%d *****testHandler::handlerMessage what =%d\n",(unsigned)pthread_self() ,message->what);
        if(9 == message->what) {
            //可能需要创建一个对象处理某个事务同时该对象中某个逻辑需要sendmessage到该线程的消息队列中进行处理
            //怎样获取looper对象其实就是Messagequeue
            //通过TLS保存looper对象这样所有子线程所有的地方都可以获取looper对象来创建handler
            Message* message = Message::obtain(1000);
            sendMessageDelayed(message,10000);
        }else if (1000 == message->what) {
            Looper::getForThread()->quit(true);//TLS 存儲looper對象Looper::getForThread()来创建其它的handler
            printf("testHandler::handlerMessag broadcast main thread %ld\n",t());
            mCondition1.broadcast();
        } else {
            //sleep(1);
        }
    }
};
//該示例是looper在子线程中的使用case
void TestHandlerAndLoop_1() {
    printf("TestHandlerAndLoop_1-begin>>>>>>>>>>> %ld\n",t());
    mCount = 10;
    NThread thread;
    TestHandler handler(thread.getLooper());
    while(mCount--) {
        Message* message = Message::obtain(mCount);
        handler.sendMessage(message);
    }
    printf("TestHandlerAndLoop_1-begin wait %ld\n",t());
    mCondition1.wait(mLock1);
    printf("TestHandlerAndLoop_1-END<<<<<<<<<<<<<< %ld\n",t());
}

void TestHandlerAndLoop_2() {
    printf("TestHandlerAndLoop_2-begin>>>>>>>>>>>>%ld\n",t());
    mCount = 10;
    NThread thread;
    TestHandler handler(thread.getLooper());
    while(mCount--) {
        Message* message = Message::obtain(mCount);
        handler.sendMessageDelayed(message,mCount*500);
    }
    printf("TestHandlerAndLoop_2-begin wait %ld\n",t());
    mCondition1.wait(mLock1);
    printf("TestHandlerAndLoop_2-END <<<<<<<<<<<<< %ld\n",t());
}
void testLooper() {
    TestHandlerAndLoop_1();
    TestHandlerAndLoop_2();

}

int main(int argc, char **argv) {
    testLooper();
    return 0;
}
