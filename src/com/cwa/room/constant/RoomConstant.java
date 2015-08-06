package com.cwa.room.constant;
/**
 * 房间常量
 * @author yangfeng
 *
 */
public class RoomConstant {
	public final static String RoomMatchingTask_Prefix = "RoomMatchingTask_";
	public final static String NotReadyTask_Prefix = "NotReadyTask_";
	
	public final static int MatchingOutTime = 20000;// 匹配房间超时20秒
	public final static int FirstMatchingOutTime = 5000;// 第一轮匹配房间超时20秒
	public final static int MatchNum = 5;//匹配时间内匹配次数
	public final static int NotReadyOutTime = 45000;// 房主不匹配房间超时10秒
	public final static int MaxRoomNum = 200;// 最大房间数
	public final static int ShowRoomNum = 5;//一次显示房间数
	public final static int PvpBattleKeyId = 1001;//pvp战场id
	public final static long ErrorTime = 15000;//战场结束误差时间
}
