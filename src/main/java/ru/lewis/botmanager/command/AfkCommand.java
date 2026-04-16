package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.CommandConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.service.AfkService;

@Component
public class AfkCommand extends CommandExecutor {

    private final AfkService afkService;
    private final CommandConfig commandConfig;

    public AfkCommand(CommandConfig config,
                      AfkService afkService) {
        super(new CommandData("afk", config.getDescriptions().getAfk()));
        this.afkService = afkService;
        this.commandConfig = config;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        afkService.sendAfkPanel(event.getChannel().asTextChannel());

        event.getHook()
                .sendMessage(commandConfig.getMessages().getAfkPanelSet())
                .queue();
    }
}