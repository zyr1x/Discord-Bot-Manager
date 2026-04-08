package ru.lewis.botmanager.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Getter
@AllArgsConstructor
public abstract class CommandExecutor {
    private CommandData commandData;

    public abstract void execute(SlashCommandInteractionEvent event);
}
