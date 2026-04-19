package ru.lewis.botmanager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("log")
@Getter
@Setter
public class LogConfig {

    /** ID канала куда слать логи. Если пусто — логи отключены */
    private String channelId;

    private Messages messages;

    @Getter
    @Setter
    public static class Messages {

        // --- Команда /log-channel ---
        private String commandDescription;
        private String setSuccess;

        // --- Заголовки логов ---
        private String titleClanAdd;
        private String titleClanRemove;
        private String titleTicketOpen;
        private String titleTicketTake;
        private String titleTicketClose;
        private String titleTicketDelete;
        private String titleEventCreate;
        private String titleEventFinish;
        private String titleReminderSet;
        private String titleMemberLeave;
        private String titleAfkSet;
        private String titleAfkCancel;

        // --- Поля ---
        private String fieldActor;
        private String fieldTarget;
        private String fieldChannel;
        private String fieldDetail;

        // --- Цвета по типу ---
        private String colorInfo;     // синий — нейтральные события
        private String colorSuccess;  // зелёный — добавления
        private String colorWarning;  // жёлтый — изменения
        private String colorDanger;   // красный — удаления/уходы

        // --- Footer ---
        private String footer;
    }
}