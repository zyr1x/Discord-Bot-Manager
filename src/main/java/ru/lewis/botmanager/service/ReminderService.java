package ru.lewis.botmanager.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;
import ru.lewis.botmanager.configuration.ReminderConfig;
import ru.lewis.botmanager.model.ReminderModel;
import ru.lewis.botmanager.repository.ReminderRepository;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReminderService {

    private static final int    MAX_PER_USER     = 10;
    private static final long   MIN_MILLIS        = 60_000L;           // 1 минута
    private static final long   MAX_MILLIS        = 30L * 24 * 3600 * 1000; // 30 дней

    // Парсит строки типа: 30m, 2h, 1d, 1h30m, 2d5h30m
    private static final Pattern TIME_PATTERN =
            Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?");

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(MOSCOW);

    private final JDA jda;
    private final ReminderConfig config;
    private final ReminderRepository repository;
    private final LogService logService;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public ReminderService(JDA jda,
                           ReminderConfig config,
                           ReminderRepository repository,
                           LogService logService) {
        this.jda = jda;
        this.config = config;
        this.repository = repository;
        this.logService = logService;
    }

    /** Запускаем тик каждые 15 секунд при старте. Подхватываем напоминания после рестарта. */
    @PostConstruct
    public void startTicker() {
        scheduler.scheduleAtFixedRate(this::tick, 15, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ─────────────────────────────────────────────
    //  Создать напоминание
    // ─────────────────────────────────────────────

    public record SetResult(boolean ok, String message) {}

    public SetResult set(String userId, String channelId, String timeRaw, String text) {
        ReminderConfig.Messages msg = config.getMessages();

        // Проверяем лимит
        long userCount = repository.getByUser(userId).size();
        if (userCount >= MAX_PER_USER) {
            return new SetResult(false,
                    msg.getErrorTooMany().replace("{max}", String.valueOf(MAX_PER_USER)));
        }

        // Парсим время
        long durationMs;
        try {
            durationMs = parseTime(timeRaw);
        } catch (IllegalArgumentException e) {
            return new SetResult(false, msg.getErrorInvalidTime());
        }

        if (durationMs < MIN_MILLIS) return new SetResult(false, msg.getErrorTooShort());
        if (durationMs > MAX_MILLIS) return new SetResult(false, msg.getErrorTooLong());

        long now = System.currentTimeMillis();
        ReminderModel reminder = new ReminderModel(
                now, userId, channelId, text, now + durationMs, now);
        repository.add(reminder);

        logService.log(LogService.Type.REMINDER_SET,
                "<@" + userId + ">", null,
                formatDuration(durationMs) + " — " + text);

        String reply = msg.getSetSuccess()
                .replace("{time}", formatDuration(durationMs))
                .replace("{text}", text);

        return new SetResult(true, reply);
    }

    // ─────────────────────────────────────────────
    //  Список напоминаний пользователя
    // ─────────────────────────────────────────────

    public String listForUser(String userId) {
        ReminderConfig.Messages msg = config.getMessages();
        List<ReminderModel> reminders = repository.getByUser(userId);

        if (reminders.isEmpty()) return msg.getListEmpty();

        StringBuilder sb = new StringBuilder("**" + msg.getListTitle() + "**\n\n");
        long now = System.currentTimeMillis();

        for (ReminderModel r : reminders) {
            long left = Math.max(0, r.getFireAtMillis() - now);
            String entry = msg.getListEntry()
                    .replace("{id}", String.valueOf(r.getId()))
                    .replace("{time_left}", formatDuration(left))
                    .replace("{text}", r.getText());
            sb.append(entry).append("\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────
    //  Отменить напоминание
    // ─────────────────────────────────────────────

    public String cancel(String userId, long reminderId) {
        ReminderConfig.Messages msg = config.getMessages();
        boolean removed = repository.remove(userId, reminderId);
        return removed ? msg.getCancelSuccess() : msg.getCancelNotFound();
    }

    // ─────────────────────────────────────────────
    //  Тик планировщика
    // ─────────────────────────────────────────────

    private void tick() {
        long now = System.currentTimeMillis();
        List<ReminderModel> due = repository.getDue(now);

        for (ReminderModel reminder : due) {
            fire(reminder);
            repository.removeById(reminder.getId());
        }
    }

    private void fire(ReminderModel reminder) {
        TextChannel channel = jda.getTextChannelById(reminder.getChannelId());
        if (channel == null) return;

        ReminderConfig.Messages msg = config.getMessages();

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(msg.getEmbedTitle())
                .setDescription(msg.getEmbedDescription()
                        .replace("{user}", "<@" + reminder.getUserId() + ">")
                        .replace("{text}", reminder.getText()))
                .setColor(parseColor(msg.getEmbedColor()))
                .setFooter(msg.getEmbedFooter()
                        .replace("{set_at}", FMT.format(Instant.ofEpochMilli(reminder.getSetAtMillis()))))
                .setTimestamp(Instant.now());

        channel.sendMessage("@everyone")
                .addEmbeds(eb.build())
                .setAllowedMentions(java.util.Collections.singleton(net.dv8tion.jda.api.entities.Message.MentionType.EVERYONE))
                .queue();
    }

    // ─────────────────────────────────────────────
    //  Утилиты
    // ─────────────────────────────────────────────

    /**
     * Парсит строки вида: 30m, 2h, 1d, 1h30m, 2d5h, 1d12h30m
     * @throws IllegalArgumentException если не удалось распарсить или всё нули
     */
    public static long parseTime(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("empty");

        Matcher m = TIME_PATTERN.matcher(raw.trim().toLowerCase());
        if (!m.matches()) throw new IllegalArgumentException("no match");

        long days    = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
        long hours   = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
        long minutes = m.group(3) != null ? Long.parseLong(m.group(3)) : 0;

        long total = (days * 86400 + hours * 3600 + minutes * 60) * 1000L;
        if (total <= 0) throw new IllegalArgumentException("zero duration");
        return total;
    }

    /** Форматирует миллисекунды в читаемую строку: "1д 2ч 30м" */
    public static String formatDuration(long ms) {
        long totalSec = ms / 1000;
        long days    = totalSec / 86400;
        long hours   = (totalSec % 86400) / 3600;
        long minutes = (totalSec % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days    > 0) sb.append(days).append("д ");
        if (hours   > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м");
        return sb.toString().trim().isEmpty() ? "< 1м" : sb.toString().trim();
    }

    private Color parseColor(String raw) {
        try { return Color.decode(raw); } catch (Exception e) { return new Color(0x5865F2); }
    }
}