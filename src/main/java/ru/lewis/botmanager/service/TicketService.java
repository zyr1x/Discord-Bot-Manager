package ru.lewis.botmanager.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.springframework.stereotype.Service;
import ru.lewis.botmanager.configuration.TicketConfig;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TicketService extends ListenerAdapter {

    private static final String BUTTON_OPEN_ID   = "ticket:open";
    private static final String BUTTON_TAKE_ID   = "ticket:take";
    private static final String BUTTON_CLOSE_ID  = "ticket:close";
    private static final String BUTTON_DELETE_ID = "ticket:delete";
    private static final String MODAL_ID         = "ticket:modal";

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(MOSCOW);

    // userId -> channelId  (чтобы не создавать дубликаты)
    private final Map<String, String> openTickets = new ConcurrentHashMap<>();

    private final TicketConfig config;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public TicketService(JDA jda, TicketConfig config) {
        this.config = config;
        jda.addEventListener(this);
    }

    // ─────────────────────────────────────────────
    //  Отправка панели через /ticket
    // ─────────────────────────────────────────────

    public void sendPanel(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        TicketConfig.Messages msg = config.getMessages();

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(msg.getPanelTitle())
                .setDescription(msg.getPanelDescription())
                .setColor(parseColor(msg.getPanelColor()))
                .setFooter(msg.getPanelFooter())
                .setTimestamp(Instant.now());

        Button openBtn = Button.primary(
                BUTTON_OPEN_ID,
                msg.getOpenButtonEmoji() + " " + msg.getOpenButtonLabel()
        );

        event.getChannel().sendMessageEmbeds(eb.build())
                .setComponents(ActionRow.of(openBtn))
                .queue();

        event.getHook().sendMessage(msg.getPanelSentSuccess()).queue();
    }

    // ─────────────────────────────────────────────
    //  Нажатие "Создать тикет"
    // ─────────────────────────────────────────────

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        switch (id) {
            case BUTTON_OPEN_ID   -> handleOpen(event);
            case BUTTON_TAKE_ID   -> handleTake(event);
            case BUTTON_CLOSE_ID  -> handleClose(event);
            case BUTTON_DELETE_ID -> handleDelete(event);
        }
    }

    private void handleOpen(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        TicketConfig.Messages msg = config.getMessages();

        // Проверяем дубликат
        if (openTickets.containsKey(userId)) {
            String existingChannelId = openTickets.get(userId);
            Guild guild = event.getGuild();
            if (guild != null && guild.getTextChannelById(existingChannelId) != null) {
                event.reply(msg.getErrorAlreadyOpen()).setEphemeral(true).queue();
                return;
            } else {
                openTickets.remove(userId);
            }
        }

        // Строим модалку из конфига
        List<TicketConfig.Question> questions = config.getQuestions();
        Modal.Builder modalBuilder = Modal.create(MODAL_ID, msg.getModalTitle());

        for (TicketConfig.Question q : questions) {
            TextInputStyle style = "PARAGRAPH".equalsIgnoreCase(q.getStyle())
                    ? TextInputStyle.PARAGRAPH
                    : TextInputStyle.SHORT;

            TextInput input = TextInput.create(q.getId(), style)
                    .setPlaceholder(q.getPlaceholder())
                    .setRequired(q.isRequired())
                    .setMinLength(q.getMinLength() > 0 ? q.getMinLength() : 1)
                    .setMaxLength(q.getMaxLength() > 0 ? q.getMaxLength() : 1024)
                    .build();

            modalBuilder.addComponents(Label.of(q.getLabel(), input));
        }

        event.replyModal(modalBuilder.build()).queue();
    }

    // ─────────────────────────────────────────────
    //  Ответ на вопросы анкеты → создаём канал
    // ─────────────────────────────────────────────

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!MODAL_ID.equals(event.getModalId())) return;

        event.deferReply().setEphemeral(true).queue();

        Guild guild = event.getGuild();
        Member member = event.getMember();
        TicketConfig.Messages msg = config.getMessages();

        if (guild == null || member == null) return;

        // Определяем категорию — берём из канала, откуда пришёл ивент
        Category category = null;
        Channel sourceChannel = guild.getTextChannelById(event.getChannelId());
        if (sourceChannel instanceof TextChannel tc && tc.getParentCategory() != null) {
            category = tc.getParentCategory();
        }

        // Имя канала: prefix-ник (без пробелов, нижний регистр)
        String effectiveName = member.getEffectiveName()
                .toLowerCase()
                .replaceAll("[^a-zа-яёA-ZА-ЯЁ0-9_\\-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        String channelName = msg.getTicketChannelPrefix() + effectiveName;

        Category finalCategory = category;

        // Создаём приватный канал
        var createAction = finalCategory != null
                ? finalCategory.createTextChannel(channelName)
                : guild.createTextChannel(channelName);

        createAction
                .addPermissionOverride(guild.getPublicRole(),
                        null,
                        EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member,
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND,
                                Permission.MESSAGE_HISTORY),
                        null)
                .queue(ticketChannel -> {
                    openTickets.put(member.getId(), ticketChannel.getId());

                    // Строим embed с ответами
                    MessageEmbed embed = buildTicketEmbed(member, event, msg);

                    Button takeBtn = Button.success(
                            BUTTON_TAKE_ID,
                            msg.getTakeButtonEmoji() + " " + msg.getTakeButtonLabel()
                    );
                    Button closeBtn = Button.secondary(
                            BUTTON_CLOSE_ID,
                            msg.getCloseButtonEmoji() + " " + msg.getCloseButtonLabel()
                    );
                    Button deleteBtn = Button.danger(
                            BUTTON_DELETE_ID,
                            msg.getDeleteButtonEmoji() + " " + msg.getDeleteButtonLabel()
                    );

                    ticketChannel.sendMessage(member.getAsMention())
                            .addEmbeds(embed)
                            .setComponents(ActionRow.of(takeBtn, closeBtn, deleteBtn))
                            .queue();

                    event.getHook().sendMessage("✅ Тикет создан: " + ticketChannel.getAsMention())
                            .queue();
                });
    }

    // ─────────────────────────────────────────────
    //  Взять в работу
    // ─────────────────────────────────────────────

    private void handleTake(ButtonInteractionEvent event) {
        TicketConfig.Messages msg = config.getMessages();

        if (!isStaff(event.getMember())) {
            event.reply(msg.getErrorNoPermission()).setEphemeral(true).queue();
            return;
        }

        String takenMsg = msg.getTakenMessage()
                .replace("{user}", event.getMember().getAsMention());

        // Отключаем кнопку "Взять" после нажатия
        event.editComponents(ActionRow.of(
                Button.success(BUTTON_TAKE_ID,
                        msg.getTakeButtonEmoji() + " " + msg.getTakeButtonLabel()).asDisabled(),
                Button.secondary(BUTTON_CLOSE_ID,
                        msg.getCloseButtonEmoji() + " " + msg.getCloseButtonLabel()),
                Button.danger(BUTTON_DELETE_ID,
                        msg.getDeleteButtonEmoji() + " " + msg.getDeleteButtonLabel())
        )).queue();

        event.getChannel().sendMessage(takenMsg).queue();
    }

    // ─────────────────────────────────────────────
    //  Закрыть (без удаления — только лок)
    // ─────────────────────────────────────────────

    private void handleClose(ButtonInteractionEvent event) {
        TicketConfig.Messages msg = config.getMessages();

        if (!isStaff(event.getMember())) {
            event.reply(msg.getErrorNoPermission()).setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        Guild guild = event.getGuild();

        if (guild != null) {
            // Убираем возможность писать для всех кроме администраторов
            channel.getPermissionOverrides().forEach(override -> {
                if (!override.isRoleOverride() ||
                        !override.getRole().hasPermission(Permission.ADMINISTRATOR)) {
                    override.getManager()
                            .deny(Permission.MESSAGE_SEND)
                            .queue();
                }
            });
        }

        String closedMsg = msg.getClosedMessage()
                .replace("{user}", event.getMember().getAsMention());

        event.editComponents(ActionRow.of(
                Button.success(BUTTON_TAKE_ID,
                        msg.getTakeButtonEmoji() + " " + msg.getTakeButtonLabel()).asDisabled(),
                Button.secondary(BUTTON_CLOSE_ID,
                        msg.getCloseButtonEmoji() + " " + msg.getCloseButtonLabel()).asDisabled(),
                Button.danger(BUTTON_DELETE_ID,
                        msg.getDeleteButtonEmoji() + " " + msg.getDeleteButtonLabel())
        )).queue();

        channel.sendMessage(closedMsg).queue();
    }

    // ─────────────────────────────────────────────
    //  Удалить канал тикета
    // ─────────────────────────────────────────────

    private void handleDelete(ButtonInteractionEvent event) {
        TicketConfig.Messages msg = config.getMessages();

        if (!isStaff(event.getMember())) {
            event.reply(msg.getErrorNoPermission()).setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        event.reply(msg.getDeletingMessage()).queue();

        // Убираем из openTickets
        openTickets.entrySet().removeIf(e -> e.getValue().equals(channel.getId()));

        scheduler.schedule(() -> channel.delete().queue(), 5, TimeUnit.SECONDS);
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private MessageEmbed buildTicketEmbed(Member member,
                                          ModalInteractionEvent event,
                                          TicketConfig.Messages msg) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(msg.getTicketEmbedTitle())
                .setDescription(msg.getTicketEmbedDescription())
                .setColor(parseColor(msg.getTicketEmbedColor()))
                .setFooter(msg.getTicketEmbedFooter())
                .setTimestamp(Instant.now())
                .setThumbnail(member.getUser().getEffectiveAvatarUrl());

        // Поле: кто создал тикет
        eb.addField(msg.getTicketUserField(), member.getAsMention(), true);
        // Поле: когда
        eb.addField(msg.getTicketCreatedAtField(),
                DATE_FMT.format(Instant.now()), true);

        eb.addField("\u200B", "\u200B", false); // spacer

        // Динамические поля из анкеты
        for (TicketConfig.Question q : config.getQuestions()) {
            var val = event.getValue(q.getId());
            String answer = (val != null && !val.getAsString().isBlank())
                    ? val.getAsString()
                    : "—";
            eb.addField(q.getLabel(), answer, false);
        }

        return eb.build();
    }

    private boolean isStaff(Member member) {
        return member != null && member.hasPermission(Permission.MANAGE_CHANNEL);
    }

    private Color parseColor(String raw) {
        if (raw == null || raw.isBlank()) return new Color(0x5865F2);
        try {
            return Color.decode(raw);
        } catch (Exception e) {
            return new Color(0x5865F2);
        }
    }
}