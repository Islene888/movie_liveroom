package com.ella.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatsDto {
    @JsonProperty("user_join")
    private int userJoin;
    @JsonProperty("like")
    private int like;
    @JsonProperty("comment")
    private int comment;
    @JsonProperty("send_gift")
    private int sendGift;

    // getter/setter
    public int getUserJoin() { return userJoin; }
    public void setUserJoin(int userJoin) { this.userJoin = userJoin; }
    public int getLike() { return like; }
    public void setLike(int like) { this.like = like; }
    public int getComment() { return comment; }
    public void setComment(int comment) { this.comment = comment; }
    public int getSendGift() { return sendGift; }
    public void setSendGift(int sendGift) { this.sendGift = sendGift; }
}
