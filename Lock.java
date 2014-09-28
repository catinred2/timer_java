/**
 * 
 */
package com.moxun.timer;
/**
 * @Description: TODO
 * @author ming
 * @date 2014年9月28日 下午7:50:23
 */
public class Lock {

	boolean isLocked = false;
	Thread lockedBy = null;
	int lockedCount = 0;
	public synchronized void lock() {
		
		Thread callingThread = Thread.currentThread();
		while (isLocked && lockedBy != callingThread) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isLocked = true;
		lockedCount++;
		lockedBy = callingThread;
	}

	public synchronized void unlock() {
		if (Thread.currentThread() == this.lockedBy) {
			lockedCount--;
			if (lockedCount == 0) {
				isLocked = false;
				notify();
			}
		}
	}

}
