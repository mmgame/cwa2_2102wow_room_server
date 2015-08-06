package com.cwa.room;

import java.util.List;
import java.util.Map;

import serverice.room.RoomInfo;
import baseice.basedao.IEntity;

import com.cwa.service.IModuleServer;

public interface IRoomService extends IModuleServer {
	RoomManager getRoomManager();

	// -----------------------------
	RoomInfo createRoom(long uid, int battleKeyId);

	boolean exitRoom(long uid);

	boolean ready(long uid);

	int getRoomByUser(long uid);

	boolean matchupOutTime(long uid);

	boolean cancelMatchupRoom(long uid);

	List<RoomInfo> getRoomList();

	RoomInfo returnBack(long uid);

	boolean kickOut(long uid, long targetId);

	boolean cancelReady(long uid);

	boolean replaceHero(long uid, List<IEntity> hids,IEntity formationEntity,Map<Integer, List<IEntity>> equipmentEntityMap);

	RoomInfo aKeyToJoin(long uid);

	boolean joinRoom(int roomId, long uid);

	RoomInfo findRoom(int rid);

	boolean matchupRoom(long uid);
}
