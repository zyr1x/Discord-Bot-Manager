package ru.lewis.botmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClanMember {
    private String userId;
    private String roleId;
    private boolean afk;
    private String afkUntil;
}
