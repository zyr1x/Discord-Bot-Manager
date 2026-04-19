package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.TicketConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.service.TicketService;

@Component
public class TicketCommand extends CommandExecutor {

    private final TicketService ticketService;

    public TicketCommand(TicketConfig config,
                         TicketService ticketService) {
        super(new CommandData(
                "ticket",
                config.getMessages().getCommandDescription()
        ));
        this.ticketService = ticketService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        ticketService.sendPanel(event);
    }
}