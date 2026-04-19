package ru.lewis.botmanager.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;
import ru.lewis.botmanager.configuration.LogConfig;
import ru.lewis.botmanager.repository.LogRepository;

import java.awt.*;
import java.time.Instant;

/**
 * Централизованный сервис логирования.
 * Использование: logService.log(LogService.Type.CLAN_ADD, actor, target, detail)
 */
@Service
public class LogService {

    public enum Type {
        CLAN_ADD, CLAN_REMOVE,
        TICKET_OPEN, TICKET_TAKE, TICKET_CLOSE, TICKET_DELETE,
        EVENT_CREATE, EVENT_FINISH,
        REMINDER_SET,
        MEMBER_LEAVE,
        AFK_SET, AFK_CANCEL
    }

    private final JDA jda;
    private final LogConfig config;
    private final LogRepository logRepository;

    public LogService(JDA jda, LogConfig config, LogRepository logRepository) {
        this.jda = jda;
        this.config = config;
        this.logRepository = logRepository;
    }

    /**
     * @param type   тип события
     * @param actor  кто совершил действие (mention или имя), может быть null
     * @param target на кого направлено (mention или имя), может быть null
     * @param detail дополнительный текст, может быть null
     */
    public void log(Type type, String actor, String target, String detail) {
        String channelId = logRepository.getChannelId();
        if (channelId == null || channelId.isBlank()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        LogConfig.Messages msg = config.getMessages();

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(resolveTitle(type, msg))
                .setColor(resolveColor(type, msg))
                .setFooter(msg.getFooter())
                .setTimestamp(Instant.now());

        if (actor != null)  eb.addField(msg.getFieldActor(),   actor,  true);
        if (target != null) eb.addField(msg.getFieldTarget(),  target, true);
        if (detail != null) eb.addField(msg.getFieldDetail(),  detail, false);

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    /** Shorthand без detail */
    public void log(Type type, String actor, String target) {
        log(type, actor, target, null);
    }

    // ─────────────────────────────────────────────

    private String resolveTitle(Type type, LogConfig.Messages msg) {
        return switch (type) {
            case CLAN_ADD      -> msg.getTitleClanAdd();
            case CLAN_REMOVE   -> msg.getTitleClanRemove();
            case TICKET_OPEN   -> msg.getTitleTicketOpen();
            case TICKET_TAKE   -> msg.getTitleTicketTake();
            case TICKET_CLOSE  -> msg.getTitleTicketClose();
            case TICKET_DELETE -> msg.getTitleTicketDelete();
            case EVENT_CREATE  -> msg.getTitleEventCreate();
            case EVENT_FINISH  -> msg.getTitleEventFinish();
            case REMINDER_SET  -> msg.getTitleReminderSet();
            case MEMBER_LEAVE  -> msg.getTitleMemberLeave();
            case AFK_SET       -> msg.getTitleAfkSet();
            case AFK_CANCEL    -> msg.getTitleAfkCancel();
        };
    }

    private Color resolveColor(Type type, LogConfig.Messages msg) {
        String hex = switch (type) {
            case CLAN_ADD, TICKET_OPEN, EVENT_CREATE, REMINDER_SET -> msg.getColorSuccess();
            case CLAN_REMOVE, TICKET_DELETE, MEMBER_LEAVE          -> msg.getColorDanger();
            case TICKET_TAKE, AFK_SET, AFK_CANCEL, EVENT_FINISH    -> msg.getColorWarning();
            case TICKET_CLOSE                                       -> msg.getColorInfo();
        };
        try { return Color.decode(hex); } catch (Exception e) { return Color.GRAY; }
    }
}