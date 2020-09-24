package com.gomson.tryangle.domain;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;

public class AccessToken {

    private long id;
    private String token;
    private Date createdAt;
    private Date expiredAt;
    private int accessCount;
    private String ip;

    public AccessToken() {
    }

    public AccessToken(long id, String token, Date createdAt, Date expiredAt, int accessCount, String ip) {
        this.id = id;
        this.token = token;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.accessCount = accessCount;
        this.ip = ip;
    }

    public AccessToken(String token, Date expiredAt) {
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getExpiredAt() {
        return expiredAt;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public String getIp() {
        return ip;
    }
}
