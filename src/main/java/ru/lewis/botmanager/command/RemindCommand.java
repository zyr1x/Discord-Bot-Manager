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

// ─────────────────────────────────────────────
//  /remind 2h30m Текст напоминания
// ─────────────────────────────────────────────
@Component
public class RemindCommand extends CommandExecutor {

    private final ReminderService reminderService;

    public RemindCommand(ReminderConfig config, ReminderService reminderService) {
        super(new CommandData(
                "remind",
                config.getMessages().getCommandDescription(),
                List.of(
                        new OptionData(OptionType.STRING, "time",
                                config.getMessages().getTimeOptionDescription(), true),
                        new OptionData(OptionType.STRING, "text",
                                config.getMessages().getTextOptionDescription(), true)
                )
        ));
        this.reminderService = reminderService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String time = event.getOption("time").getAsString();
        String text = event.getOption("text").getAsString();

        ReminderService.SetResult result = reminderService.set(
                event.getUser().getId(),
                event.getChannel().getId(),
                time, text
        );

        event.reply(result.message()).setEphemeral(true).queue();
    }
}