package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.CommandConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;

@Component
public class PingCommand extends CommandExecutor {

    public PingCommand(CommandConfig config) {
        super(new CommandData(
                "ping",
                config.getDescriptions().getPing()
        ));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.reply("Pong! 🏓").queue();
    }
}