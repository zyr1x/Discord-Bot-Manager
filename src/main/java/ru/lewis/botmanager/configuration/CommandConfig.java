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
    }

    @Getter
    @Setter
    public static class Roster {
        private String title;
        private String roleHeader;
        private String memberLine;
        private String separator;
        private String footer;
    }
}
