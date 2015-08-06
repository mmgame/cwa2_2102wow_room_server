package com.cwa.room.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cwa.component.task.ITaskManager;
import com.cwa.component.task.ITaskTypeConfig;
import com.cwa.component.task.quartz.QuartzTaskManager;
import com.cwa.component.task.quartz.config.TaskTypeConfigFactory;
import com.cwa.room.RoomManager;
import com.cwa.room.RoomService;
import com.cwa.room.constant.RoomConstant;

/**
 * 房间任务管理类
 * 
 * @author yangfeng
 *
 */
public class RoomTaskManager implements IRoomTaskManager {
	protected static final Logger logger = LoggerFactory.getLogger(IRoomTaskManager.class);

	private ITaskManager taskManager = new QuartzTaskManager("SystemJob", "SystemTrigger");
	private TaskTypeConfigFactory taskTypeConfigFactory = new TaskTypeConfigFactory();

	private RoomManager roomManager;
	private RoomService roomService;


	@Override
	public void startUp() {
		taskManager.startup();
	}
	@Override
	public void immediatelyBattleStartTask(int rid) {

	}

	@Override
	public void addRoomMatchingTask(int rid) {
		int repeatCount = RoomConstant.MatchNum;
		int intervalTime = RoomConstant.MatchingOutTime / RoomConstant.MatchNum;
		// ITaskTypeConfig simpleTypeConfig =
		// taskTypeConfigFactory.createSimpleTypeConfig(0, 0, repeatCount,
		// intervalTime);
		ITaskTypeConfig simpleTypeConfig = taskTypeConfigFactory.createSimpleTypeConfig(0, 0, 0, 0);
		RoomMatchTask task = new RoomMatchTask(rid, repeatCount);
		task.setRoomManager(roomManager);
		task.setRoomTaskManager(this);
		taskManager.addTask(task, simpleTypeConfig);
	}

	@Override
	public void removeRoomMatchingTask(int rid) {
		taskManager.deleteTask(RoomConstant.RoomMatchingTask_Prefix + rid);
	}

	@Override
	public void removeNotReadyTask(int rid) {
		taskManager.deleteTask(RoomConstant.NotReadyTask_Prefix + rid);
	}

	@Override
	public void addNotReadyTask(int rid) {
		long startTime = System.currentTimeMillis() + RoomConstant.NotReadyOutTime;
		ITaskTypeConfig simpleTypeConfig = taskTypeConfigFactory.createSimpleTypeConfig(startTime, 0, 0, 0);
		NotReadyTask task = new NotReadyTask(rid);
		task.setRoomManager(roomManager);
		task.setRoomService(roomService);
		taskManager.addTask(task, simpleTypeConfig);
	}

	// -----------------------------------------------
	public void setRoomManager(RoomManager roomManager) {
		this.roomManager = roomManager;
	}

	public void setRoomService(RoomService roomService) {
		this.roomService = roomService;
	}
}
