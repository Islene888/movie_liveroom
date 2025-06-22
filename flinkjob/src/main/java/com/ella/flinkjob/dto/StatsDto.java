package com.ella.flinkjob.dto;

public class StatsDto {
    private int userJoin;
    private int like;
    private int comment;
    private int sendGift;

    public int getUserJoin() { return userJoin; }
    public void setUserJoin(int userJoin) { this.userJoin = userJoin; }
    public int getLike() { return like; }
    public void setLike(int like) { this.like = like; }
    public int getComment() { return comment; }
    public void setComment(int comment) { this.comment = comment; }
    public int getSendGift() { return sendGift; }
    public void setSendGift(int sendGift) { this.sendGift = sendGift; }
}
