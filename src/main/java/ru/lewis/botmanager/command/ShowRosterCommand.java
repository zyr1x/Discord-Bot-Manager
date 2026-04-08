package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.CommandConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.repository.ClanRepository;
import ru.lewis.botmanager.service.RosterService;

@Component
public class ShowRosterCommand extends CommandExecutor {

    private final ClanRepository clanRepository;
    private final RosterService rosterService;
    private final CommandConfig commandConfig;

    public ShowRosterCommand(CommandConfig config,
                             ClanRepository clanRepository,
                             RosterService rosterService) {
        super(new CommandData("clan-roster", config.getDescriptions().getClanRoster()));
        this.clanRepository = clanRepository;
        this.rosterService = rosterService;
        this.commandConfig = config;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        event.getChannel().sendMessage("⏳").queue(message -> {
            clanRepository.setRosterMessage(event.getChannel().getId(), message.getId());
            rosterService.updateRosterMessage();
        });

        event.getHook().sendMessage(commandConfig.getMessages().getClanRosterSet()).queue();
    }
}