/**
 * 
 */
package com.moxun.timer;

import org.apache.log4j.Logger;

/**
 * @Description: TODO
 * @author ming
 * @date 2014年9月28日 下午3:50:41
 */
public class TimerNode {
	private Logger logger = Logger.getLogger(TimerNode.class);
	public long expire;
	public String name;
	public void execute(){
		logger.debug(name + " executed.");
	}
}
