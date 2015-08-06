package com.cwa.room.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import serverice.room.RoomInfo;
import serverice.room.RoomStateEnum;

import com.cwa.component.task.ITask;
import com.cwa.component.task.ITaskContext;
import com.cwa.room.Room;
import com.cwa.room.RoomManager;
import com.cwa.room.RoomService;
import com.cwa.room.constant.RoomConstant;
/**
 * 房间在已准备状态下房主进行匹配房间操作计时任务，超时就踢出房主
 * @author yangfeng
 *
 */
public class NotReadyTask implements ITask{
	protected static final Logger logger = LoggerFactory.getLogger(NotReadyTask.class);
	
	private int rid;
	private String taskId;
	private RoomManager roomManager;
	private RoomService roomService;
	
	public NotReadyTask(int rid) {
		this.rid = rid;
		this.taskId = RoomConstant.NotReadyTask_Prefix + rid;
	}

	@Override
	public String id() {
		return taskId;
	}

	@Override
	public void execute(ITaskContext context) {
		Room room = roomManager.getRoomInfoMap().get(rid);
		if(room != null){
			RoomInfo roomInfo = room.getRoomInfo();
			if(roomInfo.roomState != RoomStateEnum.Fighting){
				//房主没有开始匹配，从房间将房主移除
				roomService.exitRoom(roomInfo.masterId);
			}
		}
	}

	public void setRoomManager(RoomManager roomManager) {
		this.roomManager = roomManager;
	}

	public void setRoomService(RoomService roomService) {
		this.roomService = roomService;
	}
}
