package com.netty.server.netty.entity;

import com.google.common.base.Strings;
import com.google.gson.Gson;

public class UserBean extends  BaseBean{
	private static Gson gson = new Gson();
	
	public static UserBean create(String json) {
		if (!Strings.isNullOrEmpty(json)) {
			return gson.fromJson(json, UserBean.class);
		}
		return null;
	}


	@Override
	public String toJson() {
		return gson.toJson(this);
	}
	
	@Override
	public String toString() {
		return toJson();
	}

}
