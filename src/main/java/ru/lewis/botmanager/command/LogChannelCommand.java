package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.LogConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.repository.LogRepository;

@Component
public class LogChannelCommand extends CommandExecutor {

    private final LogRepository logRepository;
    private final LogConfig logConfig;

    public LogChannelCommand(LogConfig logConfig, LogRepository logRepository) {
        super(new CommandData("log-channel", logConfig.getMessages().getCommandDescription()));
        this.logRepository = logRepository;
        this.logConfig = logConfig;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        logRepository.setChannelId(event.getChannel().getId());
        event.reply(logConfig.getMessages().getSetSuccess()).setEphemeral(true).queue();
    }
}