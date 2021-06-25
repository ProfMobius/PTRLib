package com.mia.craftstudio.utils;

import java.util.HashMap;
import java.util.Map;

import com.mia.craftstudio.minecraft.forge.CSLibMod;

public class Timer {
	private Map<String, Long> timingsStart = new HashMap<String, Long>();
	private Map<String, Long> timingsStop  = new HashMap<String, Long>();
	private Map<String, Long> accumulators = new HashMap<String, Long>();
	
	public void start(final String s){
		this.timingsStart.put(s, System.currentTimeMillis());
	}

	public void stop(final String s){
		this.timingsStop.put(s, System.currentTimeMillis());
	}	

	public long get(final String s){
		return this.timingsStop.get(s) - this.timingsStart.get(s);
	}	

	public long getAcc(final String s){
		return this.accumulators.get(s);
	}		
	
	public void add(final String target, final String value){
		accumulators.put(target, accumulators.get(target) + this.get(value));
	}
	
	public void reset(final String s){
		accumulators.put(s, 0L);
	}
	
	public void print(final String s){
		CSLibMod.log.info(String.format("%s : %s ms", s, this.get(s)));
	}
	
	public void printAcc(final String s){
		CSLibMod.log.info(String.format("%s : %s ms", s, this.getAcc(s)));
	}	
}
