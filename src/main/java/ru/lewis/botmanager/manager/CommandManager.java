package ru.lewis.botmanager.manager;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.JdaConfig;
import ru.lewis.botmanager.model.CommandExecutor;

import java.util.List;

@Component
public class CommandManager {
    private final JdaConfig jdaConfig;
    private final JDA jda;
    private final List<CommandExecutor> executors;

    public CommandManager(JdaConfig jdaConfig,
                          JDA jda,
                          List<CommandExecutor> executors) {
        this.jdaConfig = jdaConfig;
        this.jda = jda;
        this.executors = executors;

        this.registerCommands();
    }

    private void registerCommands() {
        jda.getGuildById(jdaConfig.getGuildId())
                .updateCommands()
                .addCommands(
                        executors.stream()
                                .map(executor -> {
                                    var data = executor.getCommandData();
                                    var slash = Commands.slash(data.getName(), data.getDescription());
                                    if (!data.getOptions().isEmpty()) {
                                        slash.addOptions(data.getOptions());
                                    }
                                    return slash;
                                })
                                .toList()
                )
                .queue();
    }
}