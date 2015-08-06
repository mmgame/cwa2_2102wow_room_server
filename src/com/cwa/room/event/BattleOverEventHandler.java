package com.cwa.room.event;

import serverice.battle.BattleOverEvent;
import baseice.event.IEvent;

import com.cwa.component.event.IEventHandler;
import com.cwa.room.IRoomService;
import com.cwa.room.bean.RoomBattleBean;

/**
 * 匹配超时事件
 * 
 * @author tzy
 *
 */
public class BattleOverEventHandler implements IEventHandler {
	private IRoomService roomService;

	@Override
	public void eventHandler(IEvent event) {
		BattleOverEvent e = (BattleOverEvent) event;
		String battleId = e.battleId;
		if (roomService.getRoomManager().getBattleRoomMap().containsKey(battleId)) {
			RoomBattleBean bean = roomService.getRoomManager().getBattleRoomMap().get(battleId);
			bean.setOver(true);
			bean.getAttRoom().resetRoom();
			bean.getDefRoom().resetRoom();
		}
	}

	// --------------------------------------------------
	public void setRoomService(IRoomService roomService) {
		this.roomService = roomService;
	}

}
