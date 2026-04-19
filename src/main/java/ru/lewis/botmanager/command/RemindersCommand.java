package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.ReminderConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.service.ReminderService;

@Component
public class RemindersCommand extends CommandExecutor {

    private final ReminderService reminderService;

    public RemindersCommand(ReminderConfig config, ReminderService reminderService) {
        super(new CommandData("reminders", config.getMessages().getListCommandDescription()));
        this.reminderService = reminderService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String list = reminderService.listForUser(event.getUser().getId());
        event.reply(list).setEphemeral(true).queue();
    }
}