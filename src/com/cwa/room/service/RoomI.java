package com.cwa.room.service;

import java.util.List;
import java.util.Map;

import serverice.room.RoomInfo;
import serverice.room._IRoomServiceDisp;
import Ice.Current;
import baseice.basedao.IEntity;

import com.cwa.room.IRoomService;

/**
 * 房间服务
 * 
 * @author yangfeng
 *
 */
public class RoomI extends _IRoomServiceDisp {
	private static final long serialVersionUID = 1L;

	private IRoomService roomService;

	@Override
	public RoomInfo createRoom(long uid, int battleKeyId, Current __current) {
		return roomService.createRoom(uid, battleKeyId);
	}

	@Override
	public boolean exitRoom(long uid, Current __current) {
		return roomService.exitRoom(uid);
	}

	@Override
	public boolean ready(long uid, Current __current) {
		return roomService.ready(uid);
	}

	@Override
	public boolean matchupRoom(long uid, Current __current) {
		return roomService.matchupRoom(uid);
	}

	@Override
	public RoomInfo findRoom(int rid, Current __current) {
		return roomService.findRoom(rid);
	}

	@Override
	public boolean joinRoom(int roomId, long uid, Current __current) {
		return roomService.joinRoom(roomId, uid);
	}

	@Override
	public RoomInfo aKeyToJoin(long uid, Current __current) {
		return roomService.aKeyToJoin(uid);
	}

	@Override
	public boolean cancelReady(long uid, Current __current) {
		return roomService.cancelReady(uid);
	}

	@Override
	public boolean kickOut(long uid, long targetId, Current __current) {
		return roomService.kickOut(uid, targetId);
	}

	@Override
	public RoomInfo returnBack(long uid, Current __current) {
		return roomService.returnBack(uid);
	}

	@Override
	public List<RoomInfo> getRoomList(Current __current) {
		return roomService.getRoomList();
	}

	@Override
	public boolean cancelMatchupRoom(long uid, Current __current) {
		return roomService.cancelMatchupRoom(uid);
	}

	@Override
	public boolean matchupOutTime(long uid, Current __current) {
		return roomService.matchupOutTime(uid);
	}

	@Override
	public int getRoomByUser(long uid, Current __current) {
		return roomService.getRoomByUser(uid);
	}

	// ------------------------------------------------------
	public void setRoomService(IRoomService roomService) {
		this.roomService = roomService;
	}

	@Override
	public boolean replaceHero(long uid, List<IEntity> hids,
			IEntity formationEntity,
			Map<Integer, List<IEntity>> equipmentEntityMap, Current __current) {
		return roomService.replaceHero(uid, hids,formationEntity,equipmentEntityMap);
	}
}
