package com.netty.server.netty.entity;

/**
 * Created by lep on 18-12-15.
 */
public abstract class BaseBean {

    private String requestId;
    private int serviceId;
    private String username;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public abstract String toJson();

    public abstract String toString();

}
