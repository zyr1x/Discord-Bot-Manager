package ru.lewis.botmanager.manager;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.model.CommandExecutor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CommandExecutorManager extends ListenerAdapter {

    private final Map<String, CommandExecutor> executorMap;

    public CommandExecutorManager(JDA jda, List<CommandExecutor> executors) {
        this.executorMap = executors.stream()
                .collect(Collectors.toMap(
                        e -> e.getCommandData().getName(),
                        e -> e
                ));

        jda.addEventListener(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        CommandExecutor executor = executorMap.get(event.getName());

        if (executor == null) {
            event.reply("Command not found").setEphemeral(true).queue();
            return;
        }

        executor.execute(event);
    }
}