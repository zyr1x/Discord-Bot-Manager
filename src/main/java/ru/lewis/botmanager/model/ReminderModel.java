package ru.lewis.botmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReminderModel {
    private long id;           // уникальный ID (timestamp создания)
    private String userId;
    private String channelId;
    private String text;
    private long fireAtMillis; // System.currentTimeMillis() когда надо сработать
    private long setAtMillis;  // когда поставили
}