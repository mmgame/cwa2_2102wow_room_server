package com.cwa.room;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import serverice.battle.BattleInfo;
import serverice.battle.IBattleServicePrx;
import serverice.room.FightList;
import serverice.room.MatchRoomOutTimeEvent;
import serverice.room.MatchRoomSucceedEvent;
import serverice.room.RoomInfo;
import serverice.room.RoomStateEnum;
import serverice.room.TroopTypeEnum;
import serverice.room.UserActionChangeInfo;
import serverice.room.UserActionEnum;
import serverice.room.UserStateChangeEvent;
import baseice.event.IEvent;
import baseice.event.IEventListenerPrx;
import baseice.service.FunctionTypeEnum;

import com.cwa.component.functionmanage.IFunctionCluster;
import com.cwa.component.functionmanage.IFunctionService;
import com.cwa.message.RoomMessage.BattleInfoBean;
import com.cwa.room.bean.RoomBattleBean;
import com.cwa.room.bean.RoomScoreBean;
import com.cwa.room.constant.RoomConstant;
import com.cwa.service.context.IGloabalContext;

/**
 * 房间管理类
 * 
 * @author yangfeng
 * 
 */
public class RoomManager {
	private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);

	private IGloabalContext gloabalContext;

	// 所有房间信息
	private ConcurrentHashMap<Integer, Room> roomInfoMap = new ConcurrentHashMap<Integer, Room>();
	// 玩家所在房间
	private ConcurrentHashMap<Long, Integer> uidRidMap = new ConcurrentHashMap<Long, Integer>();
	// 匹配队列信息 战力，房间号
	private TreeMap<Integer, RoomScoreBean> matchRoomMap = new TreeMap<Integer, RoomScoreBean>();
	// 匹配队列信息 房间号，战力
	private ConcurrentHashMap<Integer, Integer> roomFightScoreMap = new ConcurrentHashMap<Integer, Integer>();
	// 战场队列信息 战场id，房间
	private ConcurrentHashMap<String, RoomBattleBean> battleRoomMap = new ConcurrentHashMap<String, RoomBattleBean>();

	private Random random = new Random();
	private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

	public RoomManager(IGloabalContext gloabalContext) {
		this.gloabalContext = gloabalContext;
	}

	// 获取房间战力
	public Integer getFightScoreByRoomId(int rid) {
		return roomFightScoreMap.get(rid);
	}

	// 将房间加入匹配列表
	public void putRoom(int fightScore, int rid) {
		rwl.writeLock().lock();
		try {
			RoomScoreBean bean = null;
			if (matchRoomMap.containsKey(fightScore)) {
				bean = matchRoomMap.get(fightScore);
			} else {
				bean = new RoomScoreBean(fightScore);
			}
			bean.put(rid);
			matchRoomMap.put(fightScore, bean);
			if (roomFightScoreMap.contains(rid)) {
				int score = roomFightScoreMap.remove(rid);
				bean = matchRoomMap.get(score);
				bean.remove(rid);
			}
			roomFightScoreMap.put(rid, fightScore);
		} finally {
			rwl.writeLock().unlock();
		}
	}

	// 将房间移除匹配列表
	public void removeRoom(int rid, RoomStateEnum stateEnum) {
		rwl.writeLock().lock();
		try {
			int fightCore = roomFightScoreMap.remove(rid);
			RoomScoreBean bean = matchRoomMap.get(fightCore);
			bean.remove(rid);
			if (bean.isEmpty()) {
				matchRoomMap.remove(fightCore);
			}
			setRoomState(rid, stateEnum);
			getRoomInfoMap().get(rid).setJoinTime(0L);
		} finally {
			rwl.writeLock().unlock();
		}
	}

	// 保存战场-房间队列（建立战场后调用）
	public void putBattleRoom(String battleId, int attRoomId, int defRoomId) {
		rwl.writeLock().lock();
		try {
			Room attRoom = roomInfoMap.get(attRoomId);
			Room defRoom = roomInfoMap.get(defRoomId);
			RoomBattleBean bean = new RoomBattleBean();
			bean.setAttRoom(attRoom);
			bean.setDefRoom(defRoom);
			bean.setOver(false);
			battleRoomMap.put(battleId, bean);
		} finally {
			rwl.writeLock().unlock();
		}
	}

	// 移除战场-房间队列（战场结束后调用）
	public RoomBattleBean removeBattleRoom(String battleId) {
		rwl.writeLock().lock();
		try {
			return battleRoomMap.remove(battleId);
		} finally {
			rwl.writeLock().unlock();
		}
	}

	// 获取当前房间列表（目前为获取5个）
	public List<RoomInfo> getRoomList() {
		Set<Integer> set = new HashSet<Integer>();
		List<RoomInfo> roomInfos = new ArrayList<RoomInfo>();
		if (roomInfoMap.size() <= RoomConstant.ShowRoomNum) {
			for (Room room : roomInfoMap.values()) {
				roomInfos.add(room.getRoomInfo());
			}
		} else {
			while (set.size() < RoomConstant.ShowRoomNum) {
				int index = random.nextInt(roomInfoMap.size());
				set.add(index);
			}
			int i = 0;
			for (Integer rid : roomInfoMap.keySet()) {
				if (set.contains(i)) {
					roomInfos.add(roomInfoMap.get(rid).getRoomInfo());
				}
				i++;
			}
		}
		return roomInfos;
	}

	/**
	 * 匹配房间
	 * 
	 * @param fromKey
	 * @param toKey
	 * @param rid
	 * @return
	 */
	public int randomRid(int fromKey, int toKey, int rid) {
		rwl.readLock().lock();
		try {
			Room room = roomInfoMap.get(rid);
			if (room == null) {
				return 0;
			}

			for (int j = 0; j < RoomConstant.MatchNum; j++) {
				long cTime = System.currentTimeMillis();
				if (cTime - room.getJoinTime() >= RoomConstant.MatchingOutTime) {
					// 超时由客户端来发送
					continue;
				}
				if (logger.isInfoEnabled()) {
					logger.info("matchRoomMap.size====" + matchRoomMap.size());
				}
				SortedMap<Integer, RoomScoreBean> subObjects = matchRoomMap.subMap(fromKey, toKey);
				if (logger.isInfoEnabled()) {
					logger.info("subObjects.size====" + subObjects.size());
				}
				if (subObjects.isEmpty()) {
					if (logger.isInfoEnabled()) {
						logger.info("subObjects.isEmpty()");
					}
					return 0;
				}
				int index = random.nextInt(subObjects.size());
				int i = 0;
				if (logger.isInfoEnabled()) {
					logger.info("map index-----" + index);
				}
				RoomScoreBean b = null;
				for (RoomScoreBean bean : subObjects.values()) {
					if (i == index) {
						b = bean;
						break;
					}
					i++;
				}
				Set<Integer> roomSet = b.getRoomSet();
				if (roomSet.isEmpty()) {
					if (logger.isInfoEnabled()) {
						logger.info("roomSet.isEmpty()");
					}
					return 0;
				}
				index = random.nextInt(roomSet.size());
				if (logger.isInfoEnabled()) {
					logger.info("set index-----" + index);
				}
				i = 0;
				for (Integer integer : roomSet) {
					if (i == index && integer != rid) {
						RoomInfo attRoomInfo = roomInfoMap.get(integer).getRoomInfo();
						if (attRoomInfo.roomState != RoomStateEnum.Preparation) {// 房间不是准备状态
							if (logger.isInfoEnabled()) {
								logger.info("attRoomInfo.roomState != RoomStateEnum.Preparation");
							}
							return 0;
						}
						return integer;
					}
					i++;
				}
			}
			if (logger.isInfoEnabled()) {
				logger.info("Finally!!!!!!");
			}
			return 0;
		} finally {
			rwl.readLock().unlock();
		}
	}

	/**
	 * 创建战场
	 * 
	 * @param attRid
	 * @param targetRid
	 * @return
	 */
	public BattleInfoBean createBattle(int attRid, int targetRid) {
		IFunctionService functionService = gloabalContext.getCurrentFunctionService();
		IBattleServicePrx prx = null;
		prx = functionService.getIcePrx(gloabalContext.getGid(), FunctionTypeEnum.Battle, IBattleServicePrx.class);
		if (prx == null) {
			return null;
		}
		RoomInfo attRoomInfo = roomInfoMap.get(attRid).getRoomInfo();
		List<FightList> fightLists = new ArrayList<FightList>();
		for (long userId : attRoomInfo.fightMap.keySet()) {
			FightList fightList = attRoomInfo.fightMap.get(userId);
			fightList.troopType = TroopTypeEnum.Attack;
			fightLists.add(fightList);
		}

		RoomInfo targetRoomInfo = roomInfoMap.get(targetRid).getRoomInfo();
		for (long userId : targetRoomInfo.fightMap.keySet()) {
			FightList fightList = targetRoomInfo.fightMap.get(userId);
			fightList.troopType = TroopTypeEnum.Defend;
			fightLists.add(targetRoomInfo.fightMap.get(userId));
		}
		if (attRoomInfo.roomState != RoomStateEnum.Preparation || targetRoomInfo.roomState != RoomStateEnum.Preparation) {
			return null;
		}
		BattleInfo battleInfo = prx.createBattle(attRoomInfo.battleKeyId, fightLists);
		if (battleInfo == null) {
			return null;
		}

		BattleInfoBean.Builder bean = BattleInfoBean.newBuilder();
		bean.setAttackRoomid(attRid);
		bean.setDefineRoomid(targetRid);
		bean.setIp(battleInfo.ip);
		bean.setPort(battleInfo.port);
		bean.setBattleId(battleInfo.battleId);
		return bean.build();
	}

	/**
	 * 匹配房间超时处理
	 * 
	 * @param room
	 */
	public void matchRoomOutTimeHandler(RoomInfo room) {
		sendMatchRoomOutTimeEvent(room);
	}

	/**
	 * 匹配房间成功处理
	 * 
	 * @param attRid
	 * @param deRid
	 * @param room
	 */
	public void matchRoomSeccessHandler(Room room) {
		sendMatchRoomSucceedEvent(room);
	}

	/**
	 * 房间内用户状态信息有改变处理
	 * 
	 * @param room
	 */
	public void userStateChangeHandler(long userId, UserActionEnum userActionEnum, Room room) {
		sendRoomStateChangeEvent(userId, userActionEnum, room);
	}

	public void sendMatchRoomSucceedEvent(Room room) {
		BattleInfoBean battleInfoBean = room.getBattleInfoBean();
		int attRid = battleInfoBean.getAttackRoomid();
		int deRid = battleInfoBean.getDefineRoomid();
		List<Long> uids = new ArrayList<Long>();
		for (long uid : room.getRoomInfo().fightMap.keySet()) {
			uids.add(uid);
		}
		for (long uid : room.getRoomInfo().lookers) {
			uids.add(uid);
		}
		if (attRid == room.getRoomInfo().rid) {
			for (long uid : roomInfoMap.get(deRid).getRoomInfo().fightMap.keySet()) {
				uids.add(uid);
			}
			for (long uid : roomInfoMap.get(deRid).getRoomInfo().lookers) {
				uids.add(uid);
			}
		} else {
			for (long uid : roomInfoMap.get(attRid).getRoomInfo().fightMap.keySet()) {
				uids.add(uid);
			}
			for (long uid : roomInfoMap.get(attRid).getRoomInfo().lookers) {
				uids.add(uid);
			}
		}
		MatchRoomSucceedEvent event = new MatchRoomSucceedEvent();
		event.uids = uids;
		event.attRid = attRid;
		event.deRid = deRid;
		event.ip = battleInfoBean.getIp();
		event.port = battleInfoBean.getPort();
		event.battleId = battleInfoBean.getBattleId();
		sendEvent(event);
	}

	public void sendMatchRoomOutTimeEvent(RoomInfo roomInfo) {
		List<Long> uids = new ArrayList<Long>();
		for (long uid : roomInfo.fightMap.keySet()) {
			if (uid == roomInfo.masterId) {
				continue;
			}
			uids.add(uid);
		}
		for (long uid : roomInfo.lookers) {
			uids.add(uid);
		}
		MatchRoomOutTimeEvent event = new MatchRoomOutTimeEvent();
		event.uids = uids;
		sendEvent(event);
	}

	public void sendRoomStateChangeEvent(long userId, UserActionEnum userActionEnum, Room room) {
		List<Long> uids = new ArrayList<Long>();
		if (room == null) {
			return;
		}
		for (long uid : room.getRoomInfo().fightMap.keySet()) {
			if (uid != userId) {
				uids.add(uid);
			}
		}
		for (long uid : room.getRoomInfo().lookers) {
			if (uid != userId) {
				uids.add(uid);
			}
		}
		UserStateChangeEvent event = new UserStateChangeEvent();
		event.uids = uids;
		UserActionChangeInfo uInfo = new UserActionChangeInfo();
		uInfo.uid = userId;
		uInfo.userAction = userActionEnum;
		event.uInfo = uInfo;
		sendEvent(event);
	}

	public void sendEvent(IEvent event) {
		IFunctionService functionService = gloabalContext.getCurrentFunctionService();
		IFunctionCluster functionCluster = functionService.getFunctionCluster(gloabalContext.getGid(), FunctionTypeEnum.Logic);
		IEventListenerPrx prx = functionCluster.getMasterService(IEventListenerPrx.class);
		prx.answer(event);
	}

	public Map<Integer, Room> getRoomInfoMap() {
		return roomInfoMap;
	}

	public ConcurrentHashMap<String, RoomBattleBean> getBattleRoomMap() {
		return battleRoomMap;
	}

	public Map<Long, Integer> getUidRidMap() {
		return uidRidMap;
	}

	public TreeMap<Integer, RoomScoreBean> getMatchRoomMap() {
		return matchRoomMap;
	}

	public Map<Integer, Integer> getIdMap() {
		return roomFightScoreMap;
	}

	public void setRoomState(int roomId, RoomStateEnum roomStateEnum) {
		RoomInfo roomInfo = roomInfoMap.get(roomId).getRoomInfo();
		setRoomState(roomInfo, roomStateEnum);
	}

	public void setRoomState(RoomInfo roomInfo, RoomStateEnum roomStateEnum) {
		roomInfo.roomState = roomStateEnum;
	}
}
