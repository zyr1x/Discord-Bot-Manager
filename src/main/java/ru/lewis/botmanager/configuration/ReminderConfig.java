package ru.lewis.botmanager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("reminder")
@Getter
@Setter
public class ReminderConfig {

    private Messages messages;

    @Getter
    @Setter
    public static class Messages {

        // --- Команды ---
        private String commandDescription;
        private String timeOptionDescription;
        private String textOptionDescription;
        private String listCommandDescription;
        private String cancelCommandDescription;
        private String idOptionDescription;

        // --- Ответы на /remind ---
        private String setSuccess;      // "⏰ Напомню вам через {time}: **{text}**"
        private String embedTitle;      // "⏰ Напоминание!"
        private String embedColor;
        private String embedDescription; // "{user}, вы просили напомнить:\n\n**{text}**"
        private String embedFooter;     // "Установлено: {set_at}"

        // --- /reminders list ---
        private String listTitle;
        private String listEmpty;
        private String listEntry;       // "#{id} — через {time_left}: {text}"

        // --- /reminder cancel ---
        private String cancelSuccess;
        private String cancelNotFound;

        // --- Ошибки ---
        private String errorInvalidTime;   // "❌ Неверный формат. Примеры: 30m, 2h, 1d, 1h30m"
        private String errorTooLong;       // "❌ Максимум — 30 дней"
        private String errorTooShort;      // "❌ Минимум — 1 минута"
        private String errorTooMany;       // "❌ Максимум {max} активных напоминаний"
    }
}