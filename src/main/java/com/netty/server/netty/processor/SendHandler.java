package com.netty.server.netty.processor;

import com.netty.server.netty.entity.MsgBean;
import com.netty.server.netty.handler.MyWebSocketServerHandler;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class SendHandler implements Runnable {
	private static final Logger LOG = Logger.getLogger(MyWebSocketServerHandler.class.getName());

	private SendHandler() {
	}

	private Channel channel;
	private int bufferedLength;


	private SendHandler(Channel channel, int bufferedLength) {
		this.channel = channel;
		this.bufferedLength = bufferedLength;
	}

	@Override
	public void run() {
	}

	public static void sendMsg(Channel channel, MsgBean chat) throws Exception {
		if (channel != null && channel.isActive()) {
				channel.writeAndFlush(new TextWebSocketFrame(chat.toJson()));
        }
	}
}
