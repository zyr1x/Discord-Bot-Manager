package ru.lewis.botmanager.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;
import ru.lewis.botmanager.configuration.EventConfig;
import ru.lewis.botmanager.model.EventModel;
import ru.lewis.botmanager.repository.EventRepository;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService extends ListenerAdapter {

    private static final String BTN_WILL   = "event:will";
    private static final String BTN_WONT   = "event:wont";
    private static final String BTN_MAYBE  = "event:maybe";
    private static final String BTN_FINISH = "event:finish";

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(MOSCOW);

    private final EventConfig config;
    private final EventRepository eventRepository;
    private final LogService logService;

    public EventService(JDA jda,
                        EventConfig config,
                        EventRepository eventRepository,
                        LogService logService) {
        this.config = config;
        this.eventRepository = eventRepository;
        this.logService = logService;
        jda.addEventListener(this);
    }

    // ─────────────────────────────────────────────
    //  Создание ивента через /event
    // ─────────────────────────────────────────────

    public void createEvent(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        EventConfig.Messages msg = config.getMessages();

        String name    = event.getOption("name").getAsString();
        String desc    = event.getOption("description") != null
                ? event.getOption("description").getAsString() : null;
        String timeRaw = event.getOption("time").getAsString();

        // Парсим время
        LocalDateTime scheduledAt;
        try {
            scheduledAt = LocalDateTime.parse(timeRaw,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (DateTimeParseException e) {
            event.getHook().sendMessage(msg.getErrorInvalidTime()).queue();
            return;
        }

        if (!scheduledAt.isAfter(LocalDateTime.now(MOSCOW))) {
            event.getHook().sendMessage(msg.getErrorTimeInPast()).queue();
            return;
        }

        String formattedTime = scheduledAt.format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        TextChannel channel = event.getChannel().asTextChannel();

        // Строим первоначальный embed
        MessageEmbed embed = buildEventEmbed(name, desc, formattedTime,
                List.of(), List.of(), List.of(), msg);

        channel.sendMessageEmbeds(embed)
                .setComponents(ActionRow.of(
                        Button.success(BTN_WILL,   msg.getBtnWillCome()),
                        Button.danger(BTN_WONT,    msg.getBtnWontCome()),
                        Button.secondary(BTN_MAYBE, msg.getBtnMaybe()),
                        Button.primary(BTN_FINISH,  msg.getBtnFinish())
                ))
                .queue(message -> {
                    EventModel model = new EventModel();
                    model.setId(message.getId());
                    model.setChannelId(channel.getId());
                    model.setCreatorId(event.getUser().getId());
                    model.setName(name);
                    model.setDescription(desc);
                    model.setScheduledTime(formattedTime);
                    eventRepository.add(model);

                    logService.log(LogService.Type.EVENT_CREATE,
                            event.getMember().getAsMention(), null, name);
                });

        event.getHook().sendMessage("✅ Ивент **" + name + "** создан!").queue();
    }

    // ─────────────────────────────────────────────
    //  Обработка кнопок явки
    // ─────────────────────────────────────────────

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String compId = event.getComponentId();
        if (!compId.startsWith("event:")) return;

        String messageId = event.getMessageId();
        Optional<EventModel> opt = eventRepository.findById(messageId);
        if (opt.isEmpty()) return;

        EventModel model = opt.get();
        EventConfig.Messages msg = config.getMessages();

        if (model.isFinished()) {
            event.reply("❌ Ивент уже завершён.").setEphemeral(true).queue();
            return;
        }

        String userId = event.getUser().getId();

        switch (compId) {
            case BTN_WILL  -> toggleVote(model, userId, "will");
            case BTN_WONT  -> toggleVote(model, userId, "wont");
            case BTN_MAYBE -> toggleVote(model, userId, "maybe");
            case BTN_FINISH -> {
                handleFinish(event, model, msg);
                return;
            }
        }

        eventRepository.update(model);

        // Редактируем embed
        MessageEmbed updated = buildEventEmbed(
                model.getName(), model.getDescription(), model.getScheduledTime(),
                model.getWillCome(), model.getWontCome(), model.getMaybe(), msg);

        event.editMessageEmbeds(updated).queue();
    }

    /** Переключает голос: если уже проголосовал — снимает, иначе — ставит и убирает из других */
    private void toggleVote(EventModel model, String userId, String list) {
        boolean wasIn = switch (list) {
            case "will"  -> model.getWillCome().contains(userId);
            case "wont"  -> model.getWontCome().contains(userId);
            case "maybe" -> model.getMaybe().contains(userId);
            default -> false;
        };

        // Убираем из всех
        model.getWillCome().remove(userId);
        model.getWontCome().remove(userId);
        model.getMaybe().remove(userId);

        // Если не был — добавляем (иначе просто сняли)
        if (!wasIn) {
            switch (list) {
                case "will"  -> model.getWillCome().add(userId);
                case "wont"  -> model.getWontCome().add(userId);
                case "maybe" -> model.getMaybe().add(userId);
            }
        }
    }

    private void handleFinish(ButtonInteractionEvent event, EventModel model,
                              EventConfig.Messages msg) {
        if (!event.getUser().getId().equals(model.getCreatorId())
                && !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
            event.reply(msg.getErrorOnlyCreator()).setEphemeral(true).queue();
            return;
        }

        model.setFinished(true);
        eventRepository.update(model);

        // Финальный embed со статистикой
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(msg.getFinishEmbedTitle().replace("{name}", model.getName()))
                .setColor(parseColor(msg.getFinishEmbedColor()))
                .setTimestamp(Instant.now());

        int will  = model.getWillCome().size();
        int wont  = model.getWontCome().size();
        int maybe = model.getMaybe().size();
        int total = will + wont + maybe;

        if (total == 0) {
            eb.setDescription(msg.getFinishNoParticipants());
        } else {
            eb.addField(msg.getFinishFieldTotal(),    String.valueOf(total), true);
            eb.addField(msg.getFinishFieldAttended(),  "✅ " + will,          true);
            eb.addField(msg.getFinishFieldDeclined(),  "❌ " + wont,          true);
            eb.addField(msg.getFinishFieldMaybe(),     "🤔 " + maybe,         true);

            if (!model.getWillCome().isEmpty()) {
                String list = model.getWillCome().stream()
                        .map(id -> "<@" + id + ">")
                        .collect(Collectors.joining(", "));
                eb.addField("✅ Придут", list, false);
            }
        }

        // Убираем все кнопки и ставим финальный embed
        event.editMessageEmbeds(eb.build())
                .setComponents()
                .queue();

        logService.log(LogService.Type.EVENT_FINISH,
                event.getMember().getAsMention(), null,
                model.getName() + " | Придут: " + will + ", Не придут: " + wont + ", Может: " + maybe);
    }

    // ─────────────────────────────────────────────
    //  Построение embed ивента
    // ─────────────────────────────────────────────

    private MessageEmbed buildEventEmbed(String name, String desc, String time,
                                         List<String> will, List<String> wont, List<String> maybe,
                                         EventConfig.Messages msg) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("📅 " + name)
                .setColor(parseColor(msg.getEmbedColor()))
                .setFooter(msg.getEmbedFooter())
                .setTimestamp(Instant.now());

        eb.addField(msg.getFieldTime(), "🕐 " + time, false);

        if (desc != null && !desc.isBlank()) {
            eb.addField(msg.getFieldDescription(), desc, false);
        }

        eb.addField(msg.getFieldWillCome()  + " (" + will.size()  + ")",
                formatVoters(will),  true);
        eb.addField(msg.getFieldWontCome()  + " (" + wont.size()  + ")",
                formatVoters(wont),  true);
        eb.addField(msg.getFieldMaybe()     + " (" + maybe.size() + ")",
                formatVoters(maybe), true);

        return eb.build();
    }

    private String formatVoters(List<String> ids) {
        if (ids.isEmpty()) return "—";
        return ids.stream()
                .map(id -> "<@" + id + ">")
                .collect(Collectors.joining("\n"));
    }

    private Color parseColor(String raw) {
        try { return Color.decode(raw); } catch (Exception e) { return new Color(0x5865F2); }
    }
}