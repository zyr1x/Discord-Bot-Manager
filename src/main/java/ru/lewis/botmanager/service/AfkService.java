package ru.lewis.botmanager.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.springframework.stereotype.Service;
import ru.lewis.botmanager.configuration.CommandConfig;
import ru.lewis.botmanager.repository.ClanRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class AfkService extends ListenerAdapter {

    private static final String BUTTON_TAKE_ID   = "afk:take";
    private static final String BUTTON_CANCEL_ID = "afk:cancel";
    private static final String MODAL_ID         = "afk:modal";
    private static final String INPUT_ID         = "afk:datetime";

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ClanRepository clanRepository;
    private final RosterService  rosterService;
    private final CommandConfig  commandConfig;
    private final LogService logService;

    public AfkService(JDA jda,
                      ClanRepository clanRepository,
                      RosterService rosterService,
                      CommandConfig commandConfig,
                      LogService logService) {
        this.clanRepository = clanRepository;
        this.rosterService  = rosterService;
        this.commandConfig  = commandConfig;
        this.logService = logService;
        jda.addEventListener(this);
    }

    public void sendAfkPanel(TextChannel channel) {
        CommandConfig.Messages msg = commandConfig.getMessages();
        channel.sendMessage(msg.getAfkPanelText())
                .setComponents(ActionRow.of(
                        Button.primary(BUTTON_TAKE_ID,   msg.getAfkButtonLabel()),
                        Button.danger(BUTTON_CANCEL_ID,  msg.getAfkCancelButtonLabel())
                ))
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        CommandConfig.Messages msg = commandConfig.getMessages();

        if (BUTTON_TAKE_ID.equals(id)) {
            handleTakeButton(event, msg);
        } else if (BUTTON_CANCEL_ID.equals(id)) {
            handleCancelButton(event, msg);
        }
    }

    private void handleTakeButton(ButtonInteractionEvent event, CommandConfig.Messages msg) {
        TextInput dateTimeInput = TextInput.create(INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder(msg.getAfkModalInputPlaceholder()) // "дд.мм.гггг чч:мм"
                .setMinLength(10)
                .setMaxLength(16)
                .setRequired(true)
                .build();

        Modal modal = Modal.create(MODAL_ID, msg.getAfkModalTitle())
                .addComponents(Label.of(msg.getAfkModalInputLabel(), dateTimeInput))
                .build();

        event.replyModal(modal).queue();
    }

    private void handleCancelButton(ButtonInteractionEvent event, CommandConfig.Messages msg) {
        String userId = event.getUser().getId();

        boolean inClan = clanRepository.getStorage().getMembers()
                .stream().anyMatch(m -> m.getUserId().equals(userId));

        if (!inClan) {
            event.reply(msg.getAfkNotInClan()).setEphemeral(true).queue();
            return;
        }

        boolean wasAfk = clanRepository.getStorage().getMembers()
                .stream()
                .filter(m -> m.getUserId().equals(userId))
                .anyMatch(m -> m.isAfk());

        if (!wasAfk) {
            event.reply(msg.getAfkNotAfk()).setEphemeral(true).queue();
            return;
        }

        clanRepository.cancelAfk(userId);
        logService.log(LogService.Type.AFK_CANCEL, "<@" + userId + ">", null, null);
        rosterService.updateRosterMessage();

        event.reply(msg.getAfkCancelled()).setEphemeral(true).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!MODAL_ID.equals(event.getModalId())) return;

        String userId  = event.getUser().getId();
        String rawInput = event.getValue(INPUT_ID).getAsString().trim();
        CommandConfig.Messages msg = commandConfig.getMessages();

        boolean inClan = clanRepository.getStorage().getMembers()
                .stream().anyMatch(m -> m.getUserId().equals(userId));

        if (!inClan) {
            event.reply(msg.getAfkNotInClan()).setEphemeral(true).queue();
            return;
        }

        LocalDateTime until;
        try {
            until = parseDateTime(rawInput);
        } catch (DateTimeParseException e) {
            event.reply(msg.getAfkInvalidDate()).setEphemeral(true).queue();
            return;
        }

        if (!until.isAfter(LocalDateTime.now(MOSCOW))) {
            event.reply(msg.getAfkDateInPast()).setEphemeral(true).queue();
            return;
        }

        String formatted = until.format(FORMATTER);

        clanRepository.setAfk(userId, formatted);
        logService.log(LogService.Type.AFK_SET, "<@" + userId + ">", null, "До: " + formatted);
        rosterService.updateRosterMessage();

        event.reply(msg.getAfkSuccess().replace("{date}", formatted))
                .setEphemeral(true)
                .queue();
    }

    private LocalDateTime parseDateTime(String raw) {
        // Пробуем форматы с временем
        for (String pattern : new String[]{
                "dd.MM.yyyy HH:mm",
                "dd/MM/yyyy HH:mm",
                "yyyy-MM-dd HH:mm"
        }) {
            try {
                return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {}
        }
        // Пробуем форматы только с датой — время 00:00
        for (String pattern : new String[]{
                "dd.MM.yyyy",
                "dd/MM/yyyy",
                "yyyy-MM-dd"
        }) {
            try {
                return LocalDateTime.parse(raw + " 00:00",
                        DateTimeFormatter.ofPattern(pattern + " HH:mm"));
            } catch (DateTimeParseException ignored) {}
        }
        throw new DateTimeParseException("Unrecognized format", raw, 0);
    }
}