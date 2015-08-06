package com.cwa.room.task;
/**
 * 房间任务管理接口
 * @author yangfeng
 *
 */
public interface IRoomTaskManager {
	void startUp();
	/**
	 * 立即开始任务
	 * 
	 * @param rid
	 */
	void immediatelyBattleStartTask(int rid);
	
	/**
	 * 添加房间匹配任务
	 * 
	 * @param rid
	 */
	void addRoomMatchingTask(int rid);
	
	/**
	 * 移除房间匹配任务
	 * @param rid
	 */
	void removeRoomMatchingTask(int rid);
	
	/**
	 * 添加超时未准备任务
	 * @param rid
	 */
	void addNotReadyTask(int rid);
	
	/**
	 * 移除超时未准备任务
	 * @param rid
	 */
	void removeNotReadyTask(int rid);
}
