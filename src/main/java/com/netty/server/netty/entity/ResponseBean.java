package com.netty.server.netty.entity;

import com.google.common.base.Strings;
import com.google.gson.Gson;

/**
 * Created by lep on 18-12-15.
 */
public class ResponseBean extends  MsgBean{
    private static Gson gson = new Gson();

    private boolean isSucc;

    public boolean getIsSucc(boolean isSucc){
        return this.isSucc;
    }

    public ResponseBean setIsSucc(boolean isSucc){
        this.isSucc = isSucc;
        return this;
    }

    @Override
    public String toJson() {
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public static ResponseBean create(String json) {
        if (!Strings.isNullOrEmpty(json)) {
            return gson.fromJson(json, ResponseBean.class);
        }
        return null;
    }
}
