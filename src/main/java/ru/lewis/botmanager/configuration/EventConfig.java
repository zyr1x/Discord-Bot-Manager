package ru.lewis.botmanager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("event")
@Getter
@Setter
public class EventConfig {

    private Messages messages;

    @Getter
    @Setter
    public static class Messages {

        // --- Команда /event ---
        private String commandDescription;
        private String nameOptionDescription;
        private String descOptionDescription;
        private String timeOptionDescription;

        // --- Embed ивента ---
        private String embedColor;
        private String embedFooter;
        private String fieldTime;
        private String fieldDescription;
        private String fieldWillCome;
        private String fieldWontCome;
        private String fieldMaybe;

        // --- Кнопки ---
        private String btnWillCome;
        private String btnWontCome;
        private String btnMaybe;
        private String btnFinish;

        // --- Финал ---
        private String finishEmbedTitle;
        private String finishEmbedColor;
        private String finishFieldTotal;
        private String finishFieldAttended;
        private String finishFieldDeclined;
        private String finishFieldMaybe;
        private String finishNoParticipants;

        // --- Ошибки ---
        private String errorOnlyCreator;
        private String errorInvalidTime;
        private String errorTimeInPast;
    }
}