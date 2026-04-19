package ru.lewis.botmanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventModel {
    private String id;           // messageId embed'а
    private String channelId;
    private String creatorId;
    private String name;
    private String description;
    private String scheduledTime; // "dd.MM.yyyy HH:mm"
    private List<String> willCome  = new ArrayList<>();
    private List<String> wontCome  = new ArrayList<>();
    private List<String> maybe     = new ArrayList<>();
    private boolean finished = false;
}