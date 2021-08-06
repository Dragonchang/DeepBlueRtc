package com.deepblue.rtccall.bean;

import java.io.Serializable;

/**
 * 用户信息
 */
public class UserBean implements Serializable {

    /**
     * 用户id
     */
    private Integer id;
    /**
     * 用户名称
     */
    private String name;

    /**
     * 用户头像
     */
    private String avatarUrl;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }
}
