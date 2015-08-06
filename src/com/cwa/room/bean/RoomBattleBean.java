package com.cwa.room.bean;

import com.cwa.room.Room;

public class RoomBattleBean {

	private Room attRoom;
	private Room defRoom;
	private boolean isOver;

	public Room getAttRoom() {
		return attRoom;
	}

	public void setAttRoom(Room attRoom) {
		this.attRoom = attRoom;
	}

	public Room getDefRoom() {
		return defRoom;
	}

	public void setDefRoom(Room defRoom) {
		this.defRoom = defRoom;
	}

	public boolean isOver() {
		return isOver;
	}

	public void setOver(boolean isOver) {
		this.isOver = isOver;
	}

}
