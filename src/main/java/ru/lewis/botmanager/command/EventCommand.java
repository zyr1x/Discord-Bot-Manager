package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.EventConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.service.EventService;

import java.util.List;

@Component
public class EventCommand extends CommandExecutor {

    private final EventService eventService;

    public EventCommand(EventConfig config, EventService eventService) {
        super(new CommandData(
                "event",
                config.getMessages().getCommandDescription(),
                List.of(
                        new OptionData(OptionType.STRING, "name",
                                config.getMessages().getNameOptionDescription(), true),
                        new OptionData(OptionType.STRING, "time",
                                config.getMessages().getTimeOptionDescription(), true),
                        new OptionData(OptionType.STRING, "description",
                                config.getMessages().getDescOptionDescription(), false)
                )
        ));
        this.eventService = eventService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        eventService.createEvent(event);
    }
}