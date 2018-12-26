package com.netty.server.netty.entity;

import com.google.common.base.Strings;
import com.google.gson.Gson;

/**
 * Created by lep on 18-12-15.
 */
public class MsgBean extends  BaseBean{
    private static Gson gson = new Gson();
    private String receiverUsername;
    private String message;

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getMessage() {
        return message;
    }

    public MsgBean setMessage(String message) {
        this.message = message;
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

    public static MsgBean create(String json) {
        if (!Strings.isNullOrEmpty(json)) {
            return gson.fromJson(json, MsgBean.class);
        }
        return null;
    }
}
