package com.ella.backend.dto;

public class LiveEvent {
    private String userId;
    private String eventType;
    private long timestamp;
    private EventData data; // 使用一个内部类来表示不同事件的数据


    public static class EventData {
        private String text; // For comments
        private String giftId; // For gifts
        private int value; // For gifts

    }
}