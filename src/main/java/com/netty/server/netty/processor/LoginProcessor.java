package com.netty.server.netty.processor;

import com.netty.server.netty.handler.MyWebSocketServerHandler;
import io.netty.channel.Channel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.netty.server.netty.entity.UserBean;
import com.netty.server.netty.util.ChannelCache;
import com.netty.server.netty.util.ChannelManager;

@Service
public class LoginProcessor {
	private static final Logger LOG = Logger.getLogger(MyWebSocketServerHandler.class.getName());

	private static final AtomicLong idGenerator = new AtomicLong(0);

	@Autowired
	private ChannelCache channelCache;

	private ExecutorService executorService;

	public LoginProcessor() {
		this.executorService = Executors.newSingleThreadExecutor();
	}

	/**
	 * @功能: 处理登录逻辑
	 * @param channel
	 * @param user
	 */
	public void process(Channel channel, UserBean user) {
		this.executorService.execute(new Task(channel, user));
	}

	private class Task implements Runnable {
		private Channel channel;
		private UserBean user;

		private Task(Channel channel, UserBean user) {
			this.channel = channel;
			this.user = user;
		}

		@Override
		public void run() {
			try {
				String username = this.user.getUsername();

				Long channelId = idGenerator.incrementAndGet();
				// 添加缓存
				ChannelManager.put(channelId, this.channel);
				channelCache.putChannelId(username, channelId);
				LOG.info("username :"  + username);
				LOG.info("channelId :"  + channelId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
