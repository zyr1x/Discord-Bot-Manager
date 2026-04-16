package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.CommandConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.service.EmbedService;

@Component
public class EmbedCommand extends CommandExecutor {
    private final EmbedService embedService;

    public EmbedCommand(CommandConfig config,
                        EmbedService embedService) {
        super(new CommandData("embed", config.getDescriptions().getEmbed()));
        this.embedService = embedService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        embedService.start(event);
    }
}
