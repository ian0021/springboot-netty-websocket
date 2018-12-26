package com.netty.server.netty.processor;

import com.netty.server.netty.handler.MyWebSocketServerHandler;
import io.netty.channel.Channel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.netty.server.netty.entity.UserBean;
import com.netty.server.netty.entity.MsgBean;

import com.netty.server.netty.util.ChannelCache;
import com.netty.server.netty.util.ChannelManager;


@Service
public class MsgProcessor {
	private static final Logger LOG = Logger.getLogger(MyWebSocketServerHandler.class.getName());


	private static final Map<String, LinkedBlockingQueue<MsgBean>> COMMON_QUEUE = new HashMap<String, LinkedBlockingQueue<MsgBean>>();

	@Autowired
	private ChannelCache channelCache;

	private ExecutorService executorService;

	public MsgProcessor() {
		this.executorService = Executors.newCachedThreadPool();
	}

	/**
	 * @功能: 处理消息转发逻辑
	 * @创建日期: 2015年1月7日 下午5:31:33
	 * @param channel
     *@param common  @throws InterruptedException
	 */
	public void process(Channel channel, MsgBean common) throws InterruptedException {
		// 异步处理消息,每个用户有自己的消息队列,保证了同一用户的消息是有序的
		String toUsername = common.getReceiverUsername();

		if (StringUtils.isNotBlank(toUsername)) {
			synchronized (COMMON_QUEUE) {
				if (COMMON_QUEUE.containsKey(toUsername)) {
					LinkedBlockingQueue<MsgBean> queue = COMMON_QUEUE.get(toUsername);
					queue.put(common);
				} else {
					LinkedBlockingQueue<MsgBean> queue = new LinkedBlockingQueue<MsgBean>();
					queue.put(common);
					COMMON_QUEUE.put(toUsername, queue);
					this.executorService.execute(new Task(toUsername, queue));
				}
			}
		}
	}

	private class Task implements Runnable {
		private String toUsername;
		private LinkedBlockingQueue<MsgBean> queue;

		private Task(String toUsername, LinkedBlockingQueue<MsgBean> queue) {
			this.toUsername = toUsername;
			this.queue = queue;
		}

		@Override
		public void run() {
			while (true) {
				try {
					while (!this.queue.isEmpty()) {
						MsgBean common = this.queue.poll();

						Channel channel = ChannelManager.get(Long.valueOf(channelCache.getString(this.toUsername)));
						LOG.error("id: " + channelCache.getString(this.toUsername));
						if (channel != null) {
							SendHandler.sendMsg(channel, common);
						}
						else{
							LOG.error(" channel null");
						}

				} catch (Exception e) {
					e.printStackTrace();
				}
				synchronized (COMMON_QUEUE) {
					if (this.queue.isEmpty()) {
						COMMON_QUEUE.remove(this.toUsername);
						break;
					}
				}
			}
		}
	}

}
