package ru.lewis.botmanager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties("ticket")
@Getter
@Setter
public class TicketConfig {

    /** Сообщения и тексты интерфейса */
    private Messages messages;

    /** Настройка вопросов анкеты */
    private List<Question> questions;

    @Getter
    @Setter
    public static class Messages {

        // --- Панель создания тикета ---
        private String panelTitle;
        private String panelDescription;
        private String panelFooter;
        private String panelColor;
        private String openButtonLabel;
        private String openButtonEmoji;

        // --- Команда /ticket ---
        private String commandDescription;
        private String panelSentSuccess;

        // --- Модальное окно ---
        private String modalTitle;

        // --- Канал тикета ---
        private String ticketChannelPrefix;
        private String ticketEmbedTitle;
        private String ticketEmbedDescription;
        private String ticketEmbedColor;
        private String ticketEmbedFooter;
        private String ticketUserField;
        private String ticketCreatedAtField;

        // --- Кнопки в канале тикета ---
        private String takeButtonLabel;
        private String takeButtonEmoji;
        private String closeButtonLabel;
        private String closeButtonEmoji;
        private String deleteButtonLabel;
        private String deleteButtonEmoji;

        // --- Статусы ---
        private String takenMessage;        // "Тикет взят в работу {user}"
        private String closedMessage;       // "Тикет закрыт {user}"
        private String deletingMessage;     // "Канал будет удалён через 5 секунд..."
        private String ticketLogMessage;    // лог при закрытии

        // --- Ошибки ---
        private String errorAlreadyOpen;    // "У вас уже есть открытый тикет"
        private String errorNoPermission;   // "Только администраторы могут..."
    }

    @Getter
    @Setter
    public static class Question {
        /** Уникальный ID поля (латиница, без пробелов) */
        private String id;
        /** Лейбл над полем ввода */
        private String label;
        /** Подсказка внутри поля */
        private String placeholder;
        /** SHORT или PARAGRAPH */
        private String style;
        /** Обязательное ли поле */
        private boolean required;
        /** Мин. длина */
        private int minLength;
        /** Макс. длина */
        private int maxLength;
    }
}