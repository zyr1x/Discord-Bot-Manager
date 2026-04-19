package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.ReminderConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.service.ReminderService;

import java.util.List;

@Component
public class ReminderCancelCommand extends CommandExecutor {

    private final ReminderService reminderService;

    public ReminderCancelCommand(ReminderConfig config, ReminderService reminderService) {
        super(new CommandData(
                "reminder-cancel",
                config.getMessages().getCancelCommandDescription(),
                List.of(
                        new OptionData(OptionType.INTEGER, "id",
                                config.getMessages().getIdOptionDescription(), true)
                )
        ));
        this.reminderService = reminderService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long id = event.getOption("id").getAsLong();
        String result = reminderService.cancel(event.getUser().getId(), id);
        event.reply(result).setEphemeral(true).queue();
    }
}