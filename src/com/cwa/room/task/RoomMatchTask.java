package com.cwa.room.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import serverice.room.RoomInfo;
import serverice.room.RoomStateEnum;

import com.cwa.component.task.ITask;
import com.cwa.component.task.ITaskContext;
import com.cwa.message.RoomMessage.BattleInfoBean;
import com.cwa.room.Room;
import com.cwa.room.RoomManager;
import com.cwa.room.constant.RoomConstant;
/**
 * 房间匹配任务
 * @author yangfeng
 *
 */
public class RoomMatchTask implements ITask{
	protected static final Logger logger = LoggerFactory.getLogger(RoomMatchTask.class);
	
	private int rid;
	private String taskId;
	private RoomManager roomManager;
	private RoomTaskManager roomTaskManager;
	private int repecetCount;
	private int excuteCount=-1;//任务的重复次数为实际执行次数的+1；
	
	public RoomMatchTask(int rid,int repecetCount) {
		this.rid = rid;
		this.repecetCount=repecetCount;
		this.taskId = RoomConstant.RoomMatchingTask_Prefix + rid;
	}

	@Override
	public String id() {
		return taskId;
	}

	@Override
	public void execute(ITaskContext context) {
		excuteCount++;
		Room room = roomManager.getRoomInfoMap().get(rid);
		if(room != null){
			if (logger.isInfoEnabled()) {
				logger.info("任务执行！！！！！！"+rid);
			}
			RoomInfo roomInfo = room.getRoomInfo();
			//匹配规则暂时没定
			int targetRid = roomManager.randomRid(roomInfo.fightCore - 10000, roomInfo.fightCore + 10000,rid);
			logger.info("targetRid！！！！！！"+targetRid);
			if(targetRid > 0){
				BattleInfoBean battleInfoBean = roomManager.createBattle(rid,targetRid);
				if (battleInfoBean!=null) {
					roomManager.removeRoom(targetRid,RoomStateEnum.Fighting);
					roomManager.removeRoom(rid,RoomStateEnum.Fighting);
					
					roomManager.putBattleRoom(battleInfoBean.getBattleId(), rid, targetRid);
					
					room.setBattleInfoBean(battleInfoBean);
					Room targetRoom = roomManager.getRoomInfoMap().get(targetRid);
					targetRoom.setBattleInfoBean(battleInfoBean);
					roomManager.matchRoomSeccessHandler(room);
					roomTaskManager.removeRoomMatchingTask(targetRid);
					roomTaskManager.removeRoomMatchingTask(rid);
					
					return;
				}
			}
		}
//		if (excuteCount>=repecetCount) {
//			roomManager.matchRoomOutTimeHandler(room);
//			roomManager.removeRoom(rid);
//		}
	}

	public void setRoomManager(RoomManager roomManager) {
		this.roomManager = roomManager;
	}

	public void setRoomTaskManager(RoomTaskManager roomTaskManager) {
		this.roomTaskManager = roomTaskManager;
	}
}
