/**
 * 
 */
package com.moxun.timer;

/**
 * @Description: TODO
 * @author ming
 * @date 2014年9月28日 下午5:38:52
 */
public class MyTimerTest implements Runnable{

	public static volatile int flag =1;
	public static MyTimer timer = new MyTimer();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Thread th = new Thread(new MyTimerTest());
		th.start();
		sleep(2000);
		int seconds = 4;
		timer.addTimer("44444444", seconds*1000/MyTimer.ACCURATION);
		seconds = 11;
		timer.addTimer("11111111111", seconds*1000/MyTimer.ACCURATION);
		seconds = 2;
		timer.addTimer("222222222", seconds*1000/MyTimer.ACCURATION);
//		timer.addTimer("222222", 2);
//		timer.addTimer("33333", 3);
		sleep(1000*30);
		
		flag =0;
	}
	public static void sleep(int mseconds){
		if (mseconds<=0){
			return;
		}
		try {
			Thread.sleep(mseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		while(flag==1){
			timer.updateTime();
			sleep(MyTimer.ACCURATION);
		}
		System.out.println("timer thread exit");
	}

}
