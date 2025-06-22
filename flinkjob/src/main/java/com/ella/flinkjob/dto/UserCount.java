package com.ella.flinkjob.dto;

public class UserCount {
    private String userId;    // 用户ID
    private long count;       // 该窗口内出现的次数
    private long windowEnd;   // 该窗口的结束时间戳

    // 构造函数
    public UserCount(String userId, long count, long windowEnd) {
        this.userId = userId;
        this.count = count;
        this.windowEnd = windowEnd;
    }

    // getter/setter
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public long getWindowEnd() { return windowEnd; }
    public void setWindowEnd(long windowEnd) { this.windowEnd = windowEnd; }
}
