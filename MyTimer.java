/**
 * 
 */
package com.moxun.timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.sun.org.apache.xml.internal.serializer.ElemDesc;



/**
 * @Description: TODO
 * @author ming
 * @date 2014年9月28日 下午3:46:54
 */
public class MyTimer {
	private static Logger logger = Logger.getLogger(MyTimer.class);
	private static final int TIME_NEAR_SHIFT = 8;
	private static final int TIME_NEAR = 1 << TIME_NEAR_SHIFT;
	private static final int TIME_LEVEL_SHIFT = 6;
	private static final int TIME_LEVEL = 1<<TIME_LEVEL_SHIFT;
	private static final int TIME_NEAR_MASK	= TIME_NEAR-1;
	private static final int TIME_LEVEL_MASK = TIME_LEVEL - 1;
	private static final int LEVELS = 4;
	public static final int ACCURATION = 50;//精度  单位 ms
	private ArrayList<LinkedList<TimerNode>> listNear;// = new ArrayList<LinkedList<TimerNode>>(TIME_NEAR);
	private ArrayList<ArrayList<LinkedList<TimerNode>>> t;//= new ArrayList<LinkedList<TimerNode>>(TIME_LEVEL-1);
	private Lock lock = new Lock();
	public long time;
	public long current;
	public long starttime;
	public long current_point;
	public long origin_point;
	public MyTimer(){
		listNear = new ArrayList<LinkedList<TimerNode>>(TIME_NEAR);
		for(int i=0;i<TIME_NEAR;i++){
			//LinkedList<TimerNode> list = new LinkedList<TimerNode>();
			listNear.add(null);
		}
		t = new ArrayList<ArrayList<LinkedList<TimerNode>>>(LEVELS);
		for (int j = 0; j < LEVELS; j++) {
			ArrayList<LinkedList<TimerNode>> item = new ArrayList<LinkedList<TimerNode>>(TIME_LEVEL);
			for (int i = 0; i < TIME_LEVEL ; i++) {
				item.add(null);
			}
			t.add(item);
		}
		long current = System.currentTimeMillis();
		this.starttime = current/1000;//秒数
		long cur  = current/ACCURATION;
		this.current = cur;
		this.current_point = cur;
		this.origin_point = cur;
	}
	private void linkNode(LinkedList<TimerNode> list,TimerNode node){
		list.addLast(node);
	}
	private void addNode(TimerNode node){
		long time = node.expire;
		long current_time = this.time;
		LinkedList<TimerNode> list = null;
		if ((time|TIME_NEAR_MASK) == (current_time|TIME_NEAR_MASK)){
			long idx = time & TIME_NEAR_MASK;
			list = listNear.get((int)idx);
			if (list==null){
				list = new LinkedList<TimerNode>();
				listNear.set((int)idx, list);
			}
			linkNode(list, node);
		}else{
			int i=0;
			int mask = TIME_NEAR << TIME_LEVEL_SHIFT;
			for(i=0;i<LEVELS-1;i++){
				if ( (time|(mask-1)) == (current_time|(mask-1)) ){
					break;
				}
				mask <<= TIME_LEVEL_SHIFT;
			}
			Long index = ((time>>(TIME_NEAR_SHIFT + i*TIME_LEVEL_SHIFT)) & TIME_LEVEL_MASK);
			logger.debug("t[" + i +"]" +"[" + index+"] = node " );
			list = t.get(i).get(index.intValue());
			if (list==null){
				list = new LinkedList<TimerNode>();
				t.get(i).set(index.intValue(),list);
			}
			linkNode(list, node);
		}
	}
	public void addTimer(String name,long timeout){
		TimerNode node = new TimerNode();
		node.name = name;
		lock.lock();
		try{
			node.expire = this.time + timeout;
			logger.debug("add "+name +" timeout=" + timeout +" this.time=" + this.time );
			addNode(node);
		}finally{
			lock.unlock();
		}
		
	}
	private void moveList(int level,int idx){
		LinkedList<TimerNode> list = t.get(level).get(idx);
		t.get(level).set(idx, null);
		if (list!=null && !list.isEmpty()){
			logger.debug("move t[" +level+"][" + idx +"] to NEAR");
			Iterator<TimerNode> it = list.iterator();
			while(it.hasNext()){
				addNode(it.next());
			}
		}
		else{
			logger.debug("t[" +level+"][" + idx +"] linkedlist is null or empty");
		}
		
	}
	private void timerShift(){
		lock.lock();
		try {
			
			long ct = ++this.time;
			if (ct==0){
				moveList(LEVELS-1, 0);
			}else{
				long time = ct >> TIME_NEAR_SHIFT;
				int mask = TIME_NEAR;
				int i=0;
				while((ct & (mask-1))==0){
					int idx = (int)(time & TIME_LEVEL_MASK);
					if (idx!=0){
						moveList(i, idx);
						break;
					}
					mask <<= TIME_LEVEL_SHIFT;
					time >>= TIME_LEVEL_SHIFT;
					++i;
				}
			}
		}finally{
			lock.unlock();
		}
	}
	private void invoke(LinkedList<TimerNode> list){
		if (list!=null && !list.isEmpty()){
			Iterator<TimerNode> it = list.iterator();
			while(it.hasNext()){
				TimerNode node = it.next();
				node.execute();
			}
		}
	}
	private void timerExecute(){
		lock.lock();
		try {
			int idx = (int)(this.time & TIME_NEAR_MASK);
			LinkedList<TimerNode> list = null;
			while(true){
				list=listNear.get(idx);
				if (list==null){
					break;
				}
				listNear.set(idx, null);
				if (list.isEmpty()){
					continue;
				}
				lock.unlock();
				logger.debug(" idx=" + idx);
				invoke(list);
				lock.lock();
			}
			
			
		}finally{
			lock.unlock();
		}
	}
	private void timerUpdate(){
		
		timerExecute();
		timerShift();
		timerExecute();
	}
	private long getTime(){
		return System.currentTimeMillis()/ACCURATION;
	}
	public void updateTime(){
		long cp = getTime();
		if (cp < this.current_point){
			logger.error("error:time diff change from " + cp +" to "+this.current_point);
			this.current_point = cp;
		}else if (cp != this.current_point){
			long diff = cp - this.current_point;
			this.current_point = cp;
			long oc = this.current;
			this.current +=diff;
			if (this.current<oc){
				// time rewind
				this.starttime += 0xffffffff / 100;
			}
			int i=0;
			for(i=0;i<diff;i++){
				timerUpdate();
			}
		}
	}
	
}
