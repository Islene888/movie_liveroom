package com.ella.flinkjob.dto;

public class LiveEvent {
    private String userId;
    private String eventType;
    private long timestamp;
    private EventData data; // 使用一个内部类来表示不同事件的数据

    // getter/setter...
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public EventData getData() { return data; }
    public void setData(EventData data) { this.data = data; }

    public static class EventData {
        private String text;    // For comments
        private String giftId;  // For gifts
        private int value;      // For gifts

        // getter/setter...
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getGiftId() { return giftId; }
        public void setGiftId(String giftId) { this.giftId = giftId; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
