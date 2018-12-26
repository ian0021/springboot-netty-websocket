package com.netty.server.netty.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Created by lep on 18-12-15.
 */
@Service
public class ChannelCache {
	@Autowired
	StringRedisTemplate stringRedisTemplate;

	@Resource(name="stringRedisTemplate")
	ValueOperations<String, String> valueOperations; 


    public void saveString(String key, Long value){
        valueOperations.set(key, String.valueOf(value));
    }

    public String getString(String key){
        return valueOperations.get(key);
    }

    public void deleteString(String key){
        stringRedisTemplate.delete(key);
    }

    public void putChannelId(String username, Long channelId) {
        valueOperations.set(username, String.valueOf(channelId));
    }

    public boolean contains(String username) {
        return (null != getString(username));
    }
}
