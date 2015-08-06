package com.cwa.room;

import serverice.room.CurrentBattleInfo;
import serverice.room.RoomInfo;
import serverice.room.RoomStateEnum;
import serverice.room.UserStateEnum;

import com.cwa.message.RoomMessage.BattleInfoBean;
import com.cwa.util.TimeUtil;

/**
 * 房间封装类
 * 
 * @author yangfeng
 *
 */
public class Room {
	private RoomInfo roomInfo;
	private Long joinTime;
	private BattleInfoBean battleInfoBean;

	public RoomInfo getRoomInfo() {
		return roomInfo;
	}

	public void setRoomInfo(RoomInfo roomInfo) {
		this.roomInfo = roomInfo;
	}

	public Long getJoinTime() {
		return joinTime;
	}

	public void setJoinTime(Long joinTime) {
		this.joinTime = joinTime;
	}

	public BattleInfoBean getBattleInfoBean() {
		return battleInfoBean;
	}

	public void setBattleInfoBean(BattleInfoBean battleInfoBean) {
		this.battleInfoBean = battleInfoBean;
		if (battleInfoBean == null) {
			this.roomInfo.battleInfo = null;
		} else {
			this.roomInfo.battleInfo = new CurrentBattleInfo();
			this.roomInfo.battleInfo.battleId = battleInfoBean.getBattleId();
			this.roomInfo.battleInfo.ip = battleInfoBean.getIp();
			this.roomInfo.battleInfo.port = battleInfoBean.getPort();
			this.roomInfo.battleInfo.attRoom = battleInfoBean.getAttackRoomid();
			this.roomInfo.battleInfo.defRoom = battleInfoBean.getDefineRoomid();
			this.roomInfo.battleInfo.creatTime = TimeUtil.currentSystemTime();
		}

	}

	public void resetRoom() {
		joinTime = 0L;
		battleInfoBean = null;
		roomInfo.roomState = RoomStateEnum.NotPreparation;
		roomInfo.fightCore = 0;
		roomInfo.battleInfo = null;
		for (long userId : roomInfo.stateMap.keySet()) {
			roomInfo.stateMap.put(userId, UserStateEnum.NotReady);
		}
	}
}
