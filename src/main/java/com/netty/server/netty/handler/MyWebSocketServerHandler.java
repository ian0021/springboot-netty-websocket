package com.netty.server.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


import com.netty.server.netty.entity.*;
import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import com.netty.server.netty.util.RedisDao;

import com.alibaba.fastjson.JSONObject;

import com.netty.server.netty.processor.LoginProcessor;
import com.netty.server.netty.processor.MsgProcessor;


/**
 * WebSocket服务端Handler
 *
 */
@Component
@Qualifier("myWebSocketServerHandler")
@ChannelHandler.Sharable
public class MyWebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
	private static final Logger LOG = Logger.getLogger(MyWebSocketServerHandler.class.getName());
	
	private WebSocketServerHandshaker handshaker;
	private ChannelHandlerContext ctx;
	private String sessionId;

    @Autowired
    private RedisDao redisDao;
    @Autowired
    private LoginProcessor loginProcessor;
    @Autowired
    private MsgProcessor msgProcessor;

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.error("WebSocket异常", cause);
		ctx.close();
		LOG.info(sessionId + " 	注销");
	}

	/**
	 * 处理Http请求，完成WebSocket握手<br/>
	 * 注意：WebSocket连接第一次请求使用的是Http
	 * @param ctx
	 * @param request
	 * @throws Exception
	 */
	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		// 如果HTTP解码失败，返回HHTP异常
		if (!request.getDecoderResult().isSuccess() || (!"websocket".equals(request.headers().get("Upgrade")))) {
			sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}

		// 正常WebSocket的Http连接请求，构造握手响应返回
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://" + request.headers().get(HttpHeaders.Names.HOST), null, false);
		handshaker = wsFactory.newHandshaker(request);
		if (handshaker == null) { // 无法处理的websocket版本
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else { // 向客户端发送websocket握手,完成握手
			handshaker.handshake(ctx.channel(), request);
			// 记录管道处理上下文，便于服务器推送数据到客户端
			this.ctx = ctx;
		}
	}

	/**
	 * 处理Socket请求
	 * @param ctx
	 * @param frame
	 * @throws Exception 
	 */
	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
		// 判断是否是关闭链路的指令
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}
		// 判断是否是Ping消息
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		// 当前只支持文本消息，不支持二进制消息
		if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException("当前只支持文本消息，不支持二进制消息");
		}

        String message = ((TextWebSocketFrame) frame).text();
        JSONObject json = JSONObject.parseObject(message);
        int code = json.getInteger("serviceId");
        Channel channel = ctx.channel();
        ResponseBean response = new ResponseBean();

        if (code == CODE.online.code.intValue())
        {
            UserBean request = UserBean.create(((TextWebSocketFrame)frame).text());
            response.setServiceId(request.getServiceId());
            String username = request.getUsername();
            if (Strings.isNullOrEmpty(username)) {
                response.setIsSucc(false).setMessage("name不能为空");
                return;
            } else if (redisDao.contains(username)) {
                response.setIsSucc(false).setMessage("您已经登陆了，不能重复注册");
                return;
            }
            else {
                loginProcessor.process(channel, request);
				response.setUsername(request.getUsername());
                response.setIsSucc(true).setMessage("注册成功");
            }
        }
        else if (code == CODE.send_message.code.intValue())
        {
            MsgBean request = MsgBean.create(((TextWebSocketFrame)frame).text());
            response.setServiceId(request.getServiceId());
            if (Strings.isNullOrEmpty(request.getReceiverUsername())) {
                response.setIsSucc(false).setMessage("dstname不能为空");
            } else if (Strings.isNullOrEmpty(request.getMessage())) {
                response.setIsSucc(false).setMessage("message不能为空");
            } else {
                response.setIsSucc(true).setMessage("发送消息成功");
                msgProcessor.process(channel, request);
            }
        }
        else
        {
            response.setIsSucc(false).setMessage("未知请求").toJson();
        }

        sendWebSocket(response.toJson());
	}

	/**
	 * Http返回
	 * @param ctx
	 * @param request
	 * @param response
	 */
	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
		// 返回应答给客户端
		if (response.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8);
			response.content().writeBytes(buf);
			buf.release();
			HttpHeaders.setContentLength(response, response.content().readableBytes());
		}

		// 如果是非Keep-Alive，关闭连接
		ChannelFuture f = ctx.channel().writeAndFlush(response);
		if (!HttpHeaders.isKeepAlive(request) || response.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	/**
	 */
	public void sendWebSocket(String msg) throws Exception {
		if (this.handshaker == null || this.ctx == null || this.ctx.isRemoved()) {
			throw new Exception("尚未握手成功，无法向客户端发送WebSocket消息");
		}
		this.ctx.channel().write(new TextWebSocketFrame(msg));
		this.ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		// TODO Auto-generated method stub
		if (msg instanceof FullHttpRequest) { // 传统的HTTP接入
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) { // WebSocket接入
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

}
