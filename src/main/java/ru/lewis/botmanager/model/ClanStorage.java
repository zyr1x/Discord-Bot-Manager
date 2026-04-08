package ru.lewis.botmanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ClanStorage {
    private List<ClanMember> members = new ArrayList<>();
    private String rosterMessageId;
    private String rosterChannelId;
}