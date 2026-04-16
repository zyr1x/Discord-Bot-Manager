package ru.lewis.botmanager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("command")
@Getter
@Setter
public class CommandConfig {
    private Descriptions descriptions;
    private Messages messages;
    private Roster roster;

    @Getter
    @Setter
    public static class Descriptions {
        private String ping;
        private String clanAdd;
        private String clanRemove;
        private String clanRoster;
        private String afk;
    }

    @Getter
    @Setter
    public static class Messages {
        private String clanAddSuccess;
        private String clanAddReplace;
        private String clanRemoveSuccess;
        private String clanRemoveNotFound;
        private String clanRosterSet;
        private String clanEmpty;
        private String afkPanelSet;
        private String afkPanelText;
        private String afkButtonLabel;
        private String afkModalTitle;
        private String afkModalInputLabel;
        private String afkModalInputPlaceholder;
        private String afkInvalidDate;
        private String afkDateInPast;
        private String afkNotInClan;
        private String afkSuccess;
        private String afkCancelButtonLabel;
        private String afkCancelled;
        private String afkNotAfk;
    }

    @Getter
    @Setter
    public static class Roster {
        private String title;
        private String roleHeader;
        private String memberLine;
        private String separator;
        private String footer;
        private String afkSuffix;
    }
}
