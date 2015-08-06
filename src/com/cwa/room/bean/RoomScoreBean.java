package com.cwa.room.bean;

import java.util.HashSet;
import java.util.Set;

public class RoomScoreBean {
	private int score;
	private Set<Integer> roomSet;

	public RoomScoreBean(int score) {
		this.score = score;
		roomSet = new HashSet<Integer>();
	}

	public boolean isContain(Integer id) {
		return roomSet.contains(id);
	}

	public boolean remove(int id) {
		return roomSet.remove(id);
	}

	public void put(int id) {
		roomSet.add(id);
	}

	public Set<Integer> getRoomSet() {
		return roomSet;
	}

	public int size() {
		return roomSet.size();
	}
	
	public boolean isEmpty(){
		return roomSet.isEmpty();
	}

	public double getScore() {
		return score;
	}

}
