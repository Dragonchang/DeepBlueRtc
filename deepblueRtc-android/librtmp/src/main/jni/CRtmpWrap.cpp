
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "CRtmpWrap.h"

CRtmpWrap::CRtmpWrap()
{

}

CRtmpWrap::~CRtmpWrap()
{
	RTMPAV_Close();
}

/**
 * 初始化并连接到RTMP服务器
 * @param url 服务器上对应webapp的地址
 * @param logfile log文件
 * @成功则返回 1 , 失败则返回 0
 */
int CRtmpWrap::RTMPAV_Connect(const char* url,FILE* logfile)
{
	m_pRtmp = RTMP_Alloc();
	RTMP_Init(m_pRtmp);

	RTMP_LogSetLevel(RTMP_LOGALL);
	RTMP_LogSetOutput(logfile);

	/*设置URL*/
	if (RTMP_SetupURL(m_pRtmp,(char*)url) == FALSE)
	{
		RTMP_Free(m_pRtmp);
		m_pRtmp = NULL;
		return false;
	}

    RTMP_SetBufferMS(m_pRtmp, 2 * 1024 * 1024); // 2MB
	/*连接服务器*/
	if (RTMP_Connect(m_pRtmp, NULL) == FALSE) 
	{
		RTMP_Free(m_pRtmp);
		m_pRtmp = NULL;
		return false;
	} 

	/*连接流*/
	if (RTMP_ConnectStream(m_pRtmp,0) == FALSE)
	{
		RTMP_Close(m_pRtmp);
		RTMP_Free(m_pRtmp);
		m_pRtmp = NULL;
		return false;
	}
	return true;
}
RtmpReceiver* CRtmpWrap::receivePacket()
{
	RTMPPacket packet = { 0 }, ps = { 0 };
	RTMP_Log(RTMP_LOGWARNING, "receivePacket ***********begin");
	int ret = -1;
	while ((ret = RTMP_ReadPacket(m_pRtmp, &packet))>0) {
		if(ret < 0)
		{
			RTMP_Log(RTMP_LOGWARNING, "receivePacket error");
			break;
		}
		if(ret == 0)
		{
			RTMP_Log(RTMP_LOGWARNING, "receivePacket 0");
			break;
		}
		RTMP_Log(RTMP_LOGWARNING, "RTMPPacket_IsReady 0");
		if (RTMPPacket_IsReady(&packet))
		{
			RTMP_Log(RTMP_LOGWARNING, "RTMPPacket_IsReady 1111");
			RTMP_ClientPacket(m_pRtmp, &packet);
			RTMP_Log(RTMP_LOGWARNING, "RTMPPacket_IsReady 2222222222");
			if (packet.m_packetType == RTMP_PACKET_TYPE_VIDEO) {
				RTMP_Log(RTMP_LOGWARNING, "RTMPPacket_IsReady RTMP_PACKET_TYPE_VIDEO %d",packet.m_nBodySize);
				bool keyframe = 0x17 == packet.m_body[0] ? true : false;
				bool sequence = 0x00 == packet.m_body[1];
				if(packet.m_nBodySize >0)
				{
					RTMP_Log(RTMP_LOGWARNING,"keyframe=%s, sequence=%s\n", keyframe ? "true" : "false", sequence ? "true" : "false");
					if(sequence) {
						RtmpReceiver *receiver = new RtmpReceiver();
						uint32_t offset = 10;
						uint32_t sps_num = packet.m_body[offset++] & 0x1f;
						RTMP_Log(RTMP_LOGWARNING, "*******************sps_num %d",sps_num);
						char* frame = (char *)malloc(4);
						uint32_t frameSize = 0;
						for (int i = 0; i < sps_num; i++) {
							uint8_t ch0 = packet.m_body[offset];
							uint8_t ch1 = packet.m_body[offset + 1];
							uint32_t sps_len = ((ch0 << 8) | ch1);
							offset += 2;
							// Write sps data
							memcpy(frame, nalu_header, 4);
							frameSize = frameSize + 4;
							frame = (char *)realloc(frame, sps_len);
							memcpy(frame + 4, packet.m_body + offset, sps_len);
							frameSize = frameSize + sps_len;
							offset += sps_len;
						}
						uint32_t pps_num = packet.m_body[offset++] & 0x1f;
						RTMP_Log(RTMP_LOGWARNING, "*******************pps_num %d",pps_num);
						for (int i = 0; i < pps_num; i++) {
							uint8_t ch0 = packet.m_body[offset];
							uint8_t ch1 = packet.m_body[offset + 1];
							uint32_t pps_len = ((ch0 << 8) | ch1);
							offset += 2;
							// Write pps data
							frame = (char *)realloc(frame, 4);
							memcpy(frame, nalu_header, 4);
							frameSize = frameSize + 4;
							frame = (char *)realloc(frame, pps_len);
							memcpy(frame + 4, packet.m_body + offset, pps_len);
							frameSize = frameSize + pps_len;
							offset += pps_len;
						}
						receiver->frame = frame;
						receiver->frameSize = frameSize;
						return receiver;
					} else {
						RtmpReceiver *receiver = new RtmpReceiver();
						uint32_t offset = 5;
						uint8_t ch0 = packet.m_body[offset];
						uint8_t ch1 = packet.m_body[offset + 1];
						uint8_t ch2 = packet.m_body[offset + 2];
						uint8_t ch3 = packet.m_body[offset + 3];
						uint32_t data_len = ((ch0 << 24) | (ch1 << 16) | (ch2 << 8) | ch3);
						RTMP_Log(RTMP_LOGWARNING, "*******************nalu data %d",data_len);
						char* frame = (char *)malloc(data_len);
						offset += 4;
						memcpy(frame, packet.m_body + offset, data_len);
						receiver->frame = frame;
						receiver->frameSize = data_len;
						return receiver;
					}
				}
			} else if (packet.m_packetType == RTMP_PACKET_TYPE_AUDIO) {
				RTMP_Log(RTMP_LOGWARNING, "RTMPPacket_IsReady RTMP_PACKET_TYPE_AUDIO");
				break;
			}
			break;
		} else {
			RTMP_Log(RTMP_LOGWARNING, "RTMPPacket_IsReady 继续读取chunk！！！！！！");
		}
	}

	return NULL;
}
/**
 * 发送RTMP数据包
 * @param nPacketType 数据类型
 * @param data 存储数据内容
 * @param size 数据大小
 * @param nTimestamp 当前包的时间戳
 * @成功则返回 1 , 失败则返回 0
 */
int CRtmpWrap::SendPacket(unsigned int nPacketType,unsigned char* data,unsigned int size,unsigned int nTimestamp)
{
	RTMPPacket* packet;
	/*分配包内存和初始化,len为包体长度*/
	packet = (RTMPPacket *)malloc(RTMP_HEAD_SIZE+size);
	memset(packet,0,RTMP_HEAD_SIZE);
	/*包体内存*/
	packet->m_body = (char *)packet + RTMP_HEAD_SIZE;
	packet->m_nBodySize = size;
	memcpy(packet->m_body,data,size);
	packet->m_hasAbsTimestamp = 0;
	packet->m_packetType = nPacketType; /*此处为类型有两种一种是音频,一种是视频*/
	packet->m_nInfoField2 = m_pRtmp->m_stream_id;
	packet->m_nChannel = 0x04;

	packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
	if(RTMP_PACKET_TYPE_AUDIO == nPacketType && size != 4)
	{
		packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
	}

	packet->m_nTimeStamp = nTimestamp;
	/*发送*/
	int nRet = 0;
	if(RTMP_IsConnected(m_pRtmp))
	{
		nRet = RTMP_SendPacket(m_pRtmp,packet,TRUE); /*TRUE为放进发送队列,FALSE是不放进发送队列,直接发送*/
	}

	/*释放内存*/
	free(packet);

	return nRet;
}

/**
 * 发送H264数据帧
 * @param data 存储数据帧内容
 * @param size 数据帧的大小
 * @param bIsKeyFrame 记录该帧是否为关键帧
 * @param nTimeStamp 当前帧的时间戳
 * @成功则返回 1 , 失败则返回 0
 */
int CRtmpWrap::SendH264Packet(unsigned char* data,unsigned int size,int bIsKeyFrame,unsigned int nTimeStamp)
{
	if(data == NULL && size < 11)
		return false;

    unsigned char* body = (unsigned char*)malloc(size+9);
	memset(body,0,size+9);

	int i = 0;
	if(bIsKeyFrame)
	{
		body[i++] = 0x17;// FrameType 和 CodecID
		body[i++] = 0x01;// AVCPacketType 1: AVC NALU
		//CompositionTime
		body[i++] = 0x00;  
		body[i++] = 0x00;  
		body[i++] = 0x00;

		// NALU size   
		body[i++] = size>>24 & 0xff;  
		body[i++] = size>>16 & 0xff;  
		body[i++] = size>>8 & 0xff;  
		body[i++] = size & 0xff;
		// NALU data
		memcpy(&body[i],data,size);
	}
	else
	{
		body[i++] = 0x27;// 2:Pframe  7:AVC
		body[i++] = 0x01;// AVC NALU
		//CompositionTime
		body[i++] = 0x00;
		body[i++] = 0x00;  
		body[i++] = 0x00;

		// NALU size
		body[i++] = size>>24 & 0xff;
		body[i++] = size>>16 & 0xff;
		body[i++] = size>>8 & 0xff;
		body[i++] = size & 0xff;
		// NALU data
		memcpy(&body[i],data,size);
	}

	int bRet = SendPacket(RTMP_PACKET_TYPE_VIDEO,body,i+size,nTimeStamp);
	free(body);

	return bRet;
}

/**
 * 发送视频的sps和pps信息
 * @param pps 存储视频的pps信息
 * @param pps_len 视频的pps信息长度
 * @param sps 存储视频的sps信息
 * @param sps_len 视频的sps信息长度
 * @param time 序列参数集时间
 * @成功则返回 1 , 失败则返回 0
 */
int CRtmpWrap::SendVideoSpsPps(unsigned char* pps,int pps_len,unsigned char* sps,int sps_len)
{
	RTMPPacket * packet = NULL;//rtmp包结构
	unsigned char * body = NULL;
	int i;
	packet = (RTMPPacket *)malloc(RTMP_HEAD_SIZE+1024);
	//RTMPPacket_Reset(packet);//重置packet状态
	memset(packet,0,RTMP_HEAD_SIZE+1024);
	packet->m_body = (char *)packet + RTMP_HEAD_SIZE;
	body = (unsigned char *)packet->m_body;
	i = 0;
	/*
	 * 向RTMP服务器推送视频，需要按照 FLV 的格式进行封包。因此，在我们向服务器推送第一个H264 数据包之前，
	 * 需要首先推送一个视频 Tag [AVC Sequence Header] 以下简称“视频同步包”。
	 * 该数据包含的是重要的编码信息，没有它们，解码器将无法解码。AVC sequence header结构如下:
	 * VIDEODATA
	 * Field                  Type                       Comment
	 * FrameType              UB[4]                      1 keyframe （for AVC，a seekable frame）
                                                         2 inter frame （for AVC，a nonseekable frame）
                                                         3 disposable inter frame （H.263 only）
                                                         4 generated keyframe （reserved for server use）
                                                         5 video info/command frame
	 * CodecID                UB[4]                      1 JPEG （currently unused）
	 *                                                   2 Sorenson H.263
	 *                                                   3 Screen video
	 *                                                   4 On2 VP6
	 *                                                   5 On2 VP6 with alpha channel
	 *                                                   6 Screen video version 2
	 *                                                   7 AVC
	 * VideoData             if CodecID ==2              Video frame payload or UI8
	 *                          H263VIDEOPACKET
	 *                       if CodecID ==3
	 *                          SCREENVIDEOPACKET
	 *                       if CodecID ==4
	 *                          VP6FLVVIDEOPACKET
	 *                       if CodecID ==5
	 *                          VP6FLVALPHAVIDEOPACKET
	 *                       if CodecID ==6
	 *                          SCREENV2VIDEOPACKET
	 *                       if CodecID ==7
	 *                          AVCVIDEOPACKET
	 *
	 * AVCVIDEOPACKET
	 * Field                 Type                       Comment
	 * AVCPacketType         UI8                        0: AVC sequence header
	 *                                                  1: AVC NALU
	 *                                                  2: AVC end of sequence
	 * CompositionTime       SI24                       if AVCPacketType == 1
	 *                                                     Composition time offset
	 *                                                  else
	 *                                                     0
	 * Data                 UI8[n]                      if AVCPacketType == 0
	 *                                                     AVCDecoderConfigurationRecord
	 *                                                  else if AVCPacketType == 1
	 *                                                     One or more NALUs
	 *                                                  else if AVCPacketType == 2
	 *                                                     Empty
	 */
	body[i++] = 0x17; //FrameType 和 CodecID
	//VideoData
	body[i++] = 0x00;   //AVCPacketType

	//CompositionTime
	body[i++] = 0x00;
	body[i++] = 0x00;
	body[i++] = 0x00;

	/*
	 *AVCDecoderConfigurationRecord结构，如下
	 * 长度      字段                                    说明
	 * 8 bit    configurationVersion                  版本号 1
	 * 8 bit    AVCProfileIndication                  sps[1]
	 * 8 bit    profile_compatibility                 sps[2]
	 * 8 bit    AVCLevelIndication                    sps[3]
	 * 6 bit    reserved                              111111
	 * 2 bit    lengthSizeMinusOne                    NALUnitLength的长度-1，一般为3
	 * 3 bit    reserved                              111
	 * 5 bit    numOfSequenceParameterSets            sps个数，一般为1
	 *          sequenceParameterSetNALUnits          (sps_len+sps)的数组，sps_len占2字节,紧接sps数据
	 * 8 bit    numOfPictureParameterSets             pps个数，一般为1
	 *          pictureParameterSetNALUnit            (pps_len+pps)的数组，pps_len占2字节,紧接pps数据
	 */
	/*AVCDecoderConfigurationRecord*/
	body[i++] = 0x01;       //版本号 1
	body[i++] = sps[1];
	body[i++] = sps[2];
	body[i++] = sps[3];
	body[i++] = 0xff;

	/*sps*/
	body[i++] = 0xe1;
	//sps长度采用网络字节序存储
	body[i++] = (sps_len >> 8) & 0xff;
	body[i++] = sps_len & 0xff;
	//拷贝sps数据
	memcpy(&body[i],sps,sps_len);
	i +=  sps_len;

	/*pps*/
	body[i++] = 0x01;
	//pps长度采用网络字节序存储
	body[i++] = (pps_len >> 8) & 0xff;
	body[i++] = pps_len & 0xff;

	//拷贝pps数据
	memcpy(&body[i],pps,pps_len);
	i +=  pps_len;

    /*
	 * typedef struct RTMPPacket{
	 *     uint8_t  m_headerType;
	 *     uint8_t  m_packetType;
	 *     uint8_t  m_hasAbsTimestamp;
	 *     int      m_nChannel;
	 *     uint32_t m_nTimeStamp;
	 *     int32_t  m_nInfoField2;
	 *     uint32_t m_nBodySize;
	 *     uint32_t m_nBytesRead;
	 *     RTMPChunk *m_chunk;
	 *     char    *m_body;
	 * }RTMPPacket;
	 *   
	 *   packet->m_headerType： 可以定义如下
	 *   #define RTMP_PACKET_SIZE_LARGE    0
	 *   #define RTMP_PACKET_SIZE_MEDIUM   1
	 *   #define RTMP_PACKET_SIZE_SMALL    2
	 *   #define RTMP_PACKET_SIZE_MINIMUM  3
	 *   一般定位为 RTMP_PACKET_SIZE_MEDIUM
	 *
	 *   packet->m_packetType： 音频、视频包类型
	 *   #define RTMP_PACKET_TYPE_AUDIO    0x08
	 *   #define RTMP_PACKET_TYPE_VIDEO    0x09
	 *   #define RTMP_PACKET_TYPE_INFO     0x12
	 *   还有其他更多类型，但一般都是 音频或视频
	 *
	 *   packet->m_hasAbsTimestamp： 是否使用绝对时间戳，一般定义为0
	 *
	 *   packet->m_nChannel：0x04代表source channel (invoke)
	 *
	 *   packet->m_nTimeStamp：时间戳
	 *   一般视频时间戳可以从0开始计算，(index++) * 1000/fps (index代表每帧时间戳索引 初始值为0，25fps每帧递增40；30fps递增33)
	 *   AAC每帧数据对应的采样数为1024，对应的每帧时间间隔(ms)为 1024*1000 / 采样率。
     *   音频时间戳也可以从0开始计算，48K采样每帧递增21；44.1K采样每帧递增23
	 *
	 *   packet->m_nInfoField2 = rtmp->m_stream_id
	 *
	 *   packet->m_nBodySize：RTMPPacket包体长度
	 *
	 *   packet->m_nBytesRead：不用管
	 *   packet->m_chunk： 不用管
	 *
	 *   packet->m_body：RTMPPacket包体数据，其长度为packet->m_nBodySize。
	*/

	packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
	packet->m_nBodySize = i;
	packet->m_nChannel = 0x04;
	packet->m_nTimeStamp = 0;
	packet->m_hasAbsTimestamp = 0;
	packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
	packet->m_nInfoField2 = m_pRtmp->m_stream_id;
	/*调用发送接口*/
	int nRet = RTMP_SendPacket(m_pRtmp,packet,TRUE);
	free(packet);    //释放内存
	return nRet;
}

/**
 * 发送音频关键帧
 * @param data 音频关键帧信息
 * @param len  音频关键帧信息长度
 * @成功则返回 1 , 失败则返回 0
*/
int CRtmpWrap::SendAacSpec(unsigned char* data, int spec_len)
{
	RTMPPacket* packet = NULL;//rtmp包结构
	unsigned char* body = NULL;
	int len = spec_len;//spec data长度，一般是2
	packet = (RTMPPacket*)malloc(RTMP_HEAD_SIZE+len+2);
	//RTMPPacket_Reset(packet);//重置packet状态
	memset(packet,0,RTMP_HEAD_SIZE);
	packet->m_body = (char *)packet + RTMP_HEAD_SIZE;
	body = (unsigned char *)packet->m_body;

	/*
	 * 向RTMP服务器推送音频，需要按照FLV的格式进行封包。因此，在我们向服务器推送第一个AAC数据包之前，
	 * 需要首先推送一个音频 Tag [AAC Sequence Header] 以下简称“音频同步包”
	 */
	/*AACDecoderSpecificInfo*/
	body[0] = 0xAF; //第1个字节的前4位的数值表示了音频编码类型 A代表AAC，
	                //第1个字节的第5-6位的数值表示音频采样率 此处为44 kHz
	                //第1个字节的第7位表示音频采样精度 此处音频用16位
	                //第1个字节的第8位表示音频类型，此处为立体声
	body[1] = 0x00; //AACPacketType，这个字段来表示AACAUDIODATA的类型：0 = AAC sequence header(AudioSpecificConfig)，1 = Raw AAC frame data。
	                // 第一个音频包用0，后面的都用1
	//第3，4个字节内容为AudioSpecificConfig 即 data 内容为AudioSpecificConfig，一般为2字节，
	//0x12 对应的二进制 0001 0010
	//0x10 对应的二进制 0001 0000
	//AAC sequence header存放的是AudioSpecificConfig结构，该结构则在“ISO-14496-3 Audio”中描述。
	/*
	 * AudioSpecificConfig结构的描述非常复杂，这里我做一下简化，事先设定要将要编码的音频格式，
	 * 其中，选择"AAC-LC"为音频编码，音频采样率为44100，于是AudioSpecificConfig简化为下表
	 * 长度         字段                                 说明
	 * 5 bit       audioObjectType                    编码结构类型，AAC-LC为2
	 * 4 bit       samplingFrequencyIndex             音频采样率索引值，44100HZ对应值为4
	 * 4 bit       channelConfiguration               音频输出声道,1单声道 2双声道
	 *             GASpecificConfig                   该结构包含以下三项
	 * 1 bit       frameLengthFlag                    标志位，用于表明IMDCT窗口长度，一般为0
	 * 1 bit       dependsOnCoreCoder                 标志位，表明是否依赖于corecoder，一般为0
	 * 1 bit       extensionFlag                      选择了AAC-LC，这里必须为0
	 */

	/* 我们可以将其简化并得到 AAC 音频同步包(AAC sequeuece header)的格式如下:
	 * AAC音频同步包 4Bytes
	 * AUDIODATA                                  Binaray              Hex         简称
	 * 4 bits SoundFormat                         [AAC]1010            0xA
	 * 2 bits SoundRate                           [44kHz]11
	 * 1 bits SoundSize                           [snd16bit]1          0xF         2Bytes
	 * 1 bits SoundType                           [sndStereo]1                     AACDecoderSpecific
	 *                   1Byte AACPackeyType      00000000             0x00
	 *                   5bits AudioObjectType    [AACLC]00010
	 *                   4bits SampleRateIndex    [44100]0100                      2Bytes
	 * 3Bytes SoundData  4bits ChannelConfig      [Stereo]0010         0x1210      AudioSpecificConfig
	 * [AACAudioData]    1bits FrameLengthFlag    0
	 *                   1bits dependOnCoreCoder  0
	 *                   1bits extensionFlag      0
	 *
	 * AAC音频同步包大小固定为 4 个字节。前两个字节被称为 [AACDecoderSpecificInfo]，用于描述这个音频包应当如何被解析。
	 * 后两个字节称为 [AudioSpecificConfig]，更加详细的指定了音频格式
	 */
	//AudioSpecificConfig
	memcpy(&body[2], data, len);/*data 是AudioSpecificConfig*/

	packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
	packet->m_nBodySize = len + 2;
	packet->m_nChannel = 0x05;
	packet->m_nTimeStamp = 0;
	packet->m_hasAbsTimestamp = 0;
	packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
	packet->m_nInfoField2 = m_pRtmp->m_stream_id;

	/*调用发送接口*/
	int nRet = 0;
	if(RTMP_IsConnected(m_pRtmp))
	{
		nRet = RTMP_SendPacket(m_pRtmp,packet,TRUE);
	}
	free(packet);    //释放内存
	return nRet;
}

/**
 * 发送音频数据
 * @param data 音频帧信息
 * @param len  音频帧信息长度
 * @param nTimeStamp 当前帧的时间戳
 * @成功则返回 1 , 失败则返回 0
*/
int CRtmpWrap::SendAacData(unsigned char* data, int len,unsigned int nTimeStamp)
{
    //发送AAC的普通数据，因为AAC的前7个字节（包括帧界定符）对于RTMP服务器来说是无用的
	RTMPPacket* packet = NULL;
	unsigned char* body = NULL;
	packet = (RTMPPacket*) malloc(RTMP_HEAD_SIZE + len + 2);
	memset(packet, 0, RTMP_HEAD_SIZE);
	packet->m_body = (char *) packet + RTMP_HEAD_SIZE;
	body = (unsigned char*) packet->m_body;

	/*
	 * 完成音频同步包AAC sequeuece header的推送后，我们便可向服务器推送普通的AAC数据包，推送数据包时，[AACDecoderSpecificInfo]
	 * 则变为0xAF01，向服务器说明这个包是普通AAC数据包。后面的数据为AAC原始数据去掉前7个字节（若存在CRC校验，则去掉前9个字节）
	 * AAC音频数据包(2+N) Bytes 格式如下
	 * AUDIODATA                          Binaray                 Hex        简称
	 * 4bits SoundFormat                  [AAC]1010               0xA
	 * 2bits SoundRate                    [44kHz]11
	 * 1bits SoundSize                    [snd16bit]1             0xF        2Bytes
	 * 1bits SoundType                    [sndStereo]1                       AACDecoderSpecific
	 *
	 *              1Byte AACPacketType   [AAC Raw]               0x01
	 * SoundData[AACAudioData]        N Bytes AAC Raw Data Without AAC Header
	 *                                N = [AAC Raw data length] - ([has CRC]? 9: 7)
	 */
	/*AACDecoderSpecificInfo*/
	body[0] = 0xAF;
	body[1] = 0x01;
	/*AAC Raw data*/
	memcpy(&body[2], data, len);

	packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
	packet->m_nBodySize = len + 2;
	packet->m_nChannel = 0x05;
	packet->m_nTimeStamp = nTimeStamp;
	packet->m_hasAbsTimestamp = 0;
	packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
	packet->m_nInfoField2 = m_pRtmp->m_stream_id;

	/*调用发送接口*/
	int nRet = 0;
	if(RTMP_IsConnected(m_pRtmp))
	{
		nRet = RTMP_SendPacket(m_pRtmp, packet, TRUE);
	}
	free(packet);
	return nRet;
}

/**
 * 断开连接，释放相关的资源
 */
void CRtmpWrap::RTMPAV_Close()
{
	if(m_pRtmp)  
	{
		RTMP_Close(m_pRtmp);
		RTMP_Free(m_pRtmp);
		m_pRtmp = NULL;
	}
}


