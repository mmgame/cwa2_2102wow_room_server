package com.cwa.room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import serverice.room.FightList;
import serverice.room.RoomInfo;
import serverice.room.RoomStateEnum;
import serverice.room.TroopTypeEnum;
import serverice.room.UserActionEnum;
import serverice.room.UserStateEnum;
import baseice.basedao.IEntity;

import com.cwa.component.prototype.IPrototypeClientService;
import com.cwa.prototype.BattlePrototype;
import com.cwa.room.constant.RoomConstant;
import com.cwa.room.task.RoomTaskManager;
import com.cwa.service.IService;
import com.cwa.service.constant.ServiceConstant;
import com.cwa.service.context.IGloabalContext;
import com.cwa.util.TimeUtil;

/**
 * 房间服务类
 * 
 * @author yangfeng
 * 
 */
public class RoomService implements IRoomService {
	private AtomicInteger roomIder = new AtomicInteger(0);

	private IGloabalContext gloabalContext;

	private RoomManager roomManager;
	private RoomTaskManager roomTaskManager;

	@Override
	public void startup(IGloabalContext gloabalContext) throws Exception {
		this.gloabalContext = gloabalContext;

		roomManager = new RoomManager(gloabalContext);

		roomTaskManager = new RoomTaskManager();
		roomTaskManager.startUp();
		
		roomTaskManager.setRoomManager(roomManager);
		roomTaskManager.setRoomService(this);
	}

	@Override
	public void shutdown() throws Exception {
	}

	@Override
	public RoomInfo createRoom(long uid, int battleKeyId) {
		if (roomManager.getRoomInfoMap().size() > RoomConstant.MaxRoomNum) {
			return null;
		}
		if (roomManager.getUidRidMap().containsKey(uid)) {
			return null;
		}
		int rid = roomIder.incrementAndGet();
		RoomInfo roomInfo = new RoomInfo();
		roomInfo.battleKeyId = battleKeyId;
		roomInfo.masterId = uid;
		roomInfo.rid = rid;
		// 临时写成战力为区号来保证唯一性。后面需重新修改匹配方法
		roomInfo.fightCore = rid * 10;
		roomInfo.roomState = RoomStateEnum.NotPreparation;
		roomInfo.fightMap = new HashMap<Long, FightList>();
		roomInfo.stateMap = new HashMap<Long, UserStateEnum>();
		roomInfo.lookers = new ArrayList<Long>();
		Room room = new Room();
		room.setRoomInfo(roomInfo);
		roomManager.getRoomInfoMap().put(rid, room);
		joinRoom(rid, uid);
		return roomInfo;
	}

	@Override
	public boolean joinRoom(int rid, long uid) {
		Room room = roomManager.getRoomInfoMap().get(rid);
		if (room == null) {
			return false;
		}
		RoomInfo roomInfo = room.getRoomInfo();
		if (roomInfo.roomState != RoomStateEnum.NotPreparation) {// 当前房间不为准备中
			return false;
		}

		if (roomManager.getUidRidMap().containsKey(uid)) {// 已加入别的房间
			return false;
		}
		roomManager.getUidRidMap().put(uid, rid);
		if (roomInfo.fightMap.size() >= getBattlePrototype(RoomConstant.PvpBattleKeyId).getMax()) {
			// 房间人员已满,放入观察者队列
			roomInfo.lookers.add(uid);
			return false;
		}

		FightList fightList = new FightList();
		fightList.uid = uid;
		fightList.troopType = TroopTypeEnum.Pending;// 这里先设为待定，不赋值会报错
		// TODO 选择英雄和装备
		fightList.heroIds = new ArrayList<IEntity>();
		roomInfo.fightMap.put(uid, fightList);
		roomInfo.stateMap.put(uid, UserStateEnum.NotReady);
		roomManager.userStateChangeHandler(uid, UserActionEnum.Enter, roomManager.getRoomInfoMap().get(rid));
		return true;
	}

	@Override
	public RoomInfo aKeyToJoin(long uid) {
		RoomInfo roomInfo = null;
		for (Integer rid : roomManager.getRoomInfoMap().keySet()) {
			roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
			if (roomInfo.fightMap.size() < getBattlePrototype(RoomConstant.PvpBattleKeyId).getMax()) {
				joinRoom(rid, uid);
				return roomInfo;
			}
		}
		// 没有空房间，加入一个房间当观察者
		joinRoom(roomInfo.rid, uid);
		return roomInfo;
	}

	@Override
	public boolean exitRoom(long uid) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return false;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		roomManager.getUidRidMap().remove(uid);
		RoomInfo roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
		if (roomInfo == null) {
			return false;
		}
		if (roomInfo.lookers.contains(uid)) {
			// 房间的观看者
			roomInfo.lookers.remove(uid);
			roomManager.userStateChangeHandler(uid, UserActionEnum.Exit, roomManager.getRoomInfoMap().get(rid));
			return false;
		}
		// TODO 玩家状态为准备中或者房间状态为匹配或者战斗时不能退出
		if (roomInfo.stateMap.get(uid) == null || roomInfo.stateMap.get(uid) == UserStateEnum.Ready) {
			return false;
		}
		if (roomInfo.roomState != RoomStateEnum.NotPreparation) {// 当前房间不为准备中
			return false;
		}
		// 不是观察者
		roomTaskManager.removeNotReadyTask(rid);// 移除开始计时任务
		roomInfo.stateMap.remove(uid);
		long masterId = roomInfo.masterId;
		if (uid == masterId) {
			// 房主退出
			Map<Long, FightList> map = roomInfo.fightMap;
			map.remove(uid);
			if (map.isEmpty()) {
				roomManager.getRoomInfoMap().remove(rid);
			} else {
				for (long id : map.keySet()) {
					roomInfo.masterId = id;
					break;
				}
			}
		} else {
			roomInfo.fightMap.remove(uid);
		}
		roomManager.userStateChangeHandler(uid, UserActionEnum.Exit, roomManager.getRoomInfoMap().get(rid));
		return true;
	}

	@Override
	public boolean ready(long uid) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return false;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		RoomInfo roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
		if (roomInfo.roomState != RoomStateEnum.NotPreparation) {
			return false;
		}
		if (!roomInfo.fightMap.containsKey(uid)) {
			return false;
		}
		if (roomInfo.stateMap.get(uid) != null && roomInfo.stateMap.get(uid) == UserStateEnum.Ready) {
			return false;
		}
		if (roomInfo.masterId == uid) {
			// uid是房主,不能准备
			return false;
		}
		if (roomInfo.fightMap.get(uid).heroIds.isEmpty()) {
			return false;
		}
		roomInfo.stateMap.put(uid, UserStateEnum.Ready);
		roomManager.userStateChangeHandler(uid, UserActionEnum.GoReady, roomManager.getRoomInfoMap().get(rid));
		if (roomInfo.roomState == RoomStateEnum.NotPreparation) {
			for (long userId : roomInfo.stateMap.keySet()) {
				if (roomInfo.stateMap.get(userId) != UserStateEnum.Ready) {
					return false;
				}
			}
			roomTaskManager.addNotReadyTask(rid);// 添加开始计时任务
		}
		return true;
	}

	@Override
	public boolean cancelReady(long uid) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return false;
		}
		int rid = roomManager.getUidRidMap().get(uid);

		RoomInfo roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
		if (roomInfo.roomState != RoomStateEnum.NotPreparation) {
			return false;
		}
		if (!roomInfo.fightMap.containsKey(uid)) {
			return false;
		}
		if (roomInfo.stateMap.get(uid) == UserStateEnum.NotReady) {
			return false;
		}
		roomTaskManager.removeNotReadyTask(rid);// 移除开始计时任务
		roomInfo.stateMap.put(uid, UserStateEnum.NotReady);
		roomManager.userStateChangeHandler(uid, UserActionEnum.CancelReady, roomManager.getRoomInfoMap().get(rid));
		return true;
	}

	@Override
	public boolean matchupRoom(long uid) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return false;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		RoomInfo roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
		if (roomInfo.masterId != uid) {
			// uid不是房主
			return false;
		}
		if (roomInfo.roomState != RoomStateEnum.NotPreparation) {
			return false;
		}
		for (long userId : roomInfo.stateMap.keySet()) {
			if (userId != roomInfo.masterId && roomInfo.stateMap.get(userId) != UserStateEnum.Ready) {
				return false;
			}
		}
		if (roomInfo.fightMap.get(uid).heroIds.isEmpty()) {
			return false;
		}

		{
			roomInfo.fightCore = rid * 10;
			// TODO 这里应该计算房间总战力
		}
		roomInfo.roomState = RoomStateEnum.Preparation;
		roomManager.getRoomInfoMap().get(rid).setJoinTime(System.currentTimeMillis());
		roomTaskManager.removeNotReadyTask(rid);// 移除开始计时任务
		roomManager.putRoom(roomInfo.fightCore, rid);
		// 加入任务
		roomTaskManager.addRoomMatchingTask(rid);
		roomManager.userStateChangeHandler(uid, UserActionEnum.Matchup, roomManager.getRoomInfoMap().get(rid));
		return true;

	}

	@Override
	public boolean matchupOutTime(long uid) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return false;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		RoomInfo roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
		if (roomInfo.masterId != uid) {
			// uid不是房主
			return false;
		}
		if (roomInfo.roomState != RoomStateEnum.Preparation) {
			// 不是匹配中不能取消
			return false;
		}
		roomManager.matchRoomOutTimeHandler(roomInfo);
		roomManager.removeRoom(rid, RoomStateEnum.NotPreparation);
		return true;
	}

	@Override
	public boolean cancelMatchupRoom(long uid) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return false;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		RoomInfo roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
		if (roomInfo.masterId != uid) {
			// uid不是房主
			return false;
		}
		if (roomInfo.roomState != RoomStateEnum.Preparation) {
			// 不是匹配中不能取消
			return false;
		}
		roomManager.removeRoom(rid, RoomStateEnum.NotPreparation);
		roomTaskManager.addNotReadyTask(rid);// 添加开始计时任务
		roomManager.userStateChangeHandler(uid, UserActionEnum.CancelMatchup, roomManager.getRoomInfoMap().get(rid));
		return true;

	}

	@Override
	public RoomInfo findRoom(int rid) {
		Room room = roomManager.getRoomInfoMap().get(rid);
		if (room == null) {
			return null;
		}
		// 如果当前房间为战斗中，则判断战场是否超过时间，超过则充值房间战场信息
		if (room.getRoomInfo().roomState == RoomStateEnum.Fighting) {
			int battleId = room.getRoomInfo().battleKeyId;
			BattlePrototype battlePrototype = getBattlePrototype(battleId);
			long creatTime = room.getRoomInfo().battleInfo.creatTime;
			long battleTime = battlePrototype.getBattleTime();
			long currentTime = TimeUtil.currentSystemTime();
			long s = creatTime + battleTime + RoomConstant.ErrorTime;
			System.out.println(s);
			System.out.println(currentTime);
			if (s < currentTime) {
				room.resetRoom();
			}
		}
		return room.getRoomInfo();
	}

	@Override
	public int getRoomByUser(long uid) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return 0;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		return rid;
	}

	@Override
	public boolean replaceHero(long uid, List<IEntity> hids,IEntity formationEntity,Map<Integer, List<IEntity>> equipmentEntityMap) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return false;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		RoomInfo roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
		if (roomInfo.roomState != RoomStateEnum.NotPreparation) {
			return false;
		}
		if (!roomInfo.fightMap.containsKey(uid)) {
			return false;
		}
		if (roomInfo.stateMap.get(uid) != UserStateEnum.NotReady) {
			return false;
		}
		FightList fightList = roomInfo.fightMap.get(uid);
		fightList.heroIds = hids;
		fightList.equipmentEntityMap = equipmentEntityMap;
		fightList.formationEntity = formationEntity;
		return true;

	}

	@Override
	public boolean kickOut(long uid, long targetId) {
		if ((!roomManager.getUidRidMap().containsKey(uid)) || (!roomManager.getUidRidMap().containsKey(targetId))) {
			return false;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		int targetRid = roomManager.getUidRidMap().get(targetId);
		if (targetRid != rid) {
			return false;
		}
		RoomInfo roomInfo = roomManager.getRoomInfoMap().get(rid).getRoomInfo();
		if (roomInfo.masterId != uid) {
			// uid不是房主
			return false;
		}
		if (roomInfo.fightMap.containsKey(targetId)) {
			roomTaskManager.removeNotReadyTask(rid);// 移除开始计时任务
		}
		return exitRoom(targetId);
	}

	@Override
	public RoomInfo returnBack(long uid) {
		if (!roomManager.getUidRidMap().containsKey(uid)) {
			return null;
		}
		int rid = roomManager.getUidRidMap().get(uid);
		roomManager.getRoomInfoMap().get(rid).setBattleInfoBean(null);
		roomManager.getRoomInfoMap().get(rid).setJoinTime(0L);
		roomManager.getRoomInfoMap().get(rid).getRoomInfo().roomState = RoomStateEnum.NotPreparation;
		return roomManager.getRoomInfoMap().get(rid).getRoomInfo();
	}

	@Override
	public List<RoomInfo> getRoomList() {
		return roomManager.getRoomList();
	}

	public BattlePrototype getBattlePrototype(int keyId) {
		IService service = gloabalContext.getCurrentService(ServiceConstant.ProtoclientKey);
		if (service == null) {
			return null;
		}
		IPrototypeClientService prototypeMgr = (IPrototypeClientService) service;
		return prototypeMgr.getPrototype(BattlePrototype.class, keyId);
	}

	@Override
	public RoomManager getRoomManager() {
		return roomManager;
	}
}
