package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.CommandConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.repository.ClanRepository;
import ru.lewis.botmanager.service.RosterService;
import ru.lewis.botmanager.utils.MessageFormatter;

import java.util.List;

@Component
public class RemoveMemberCommand extends CommandExecutor {

    private final ClanRepository clanRepository;
    private final RosterService rosterService;
    private final CommandConfig commandConfig;
    private final MessageFormatter formatter;

    public RemoveMemberCommand(CommandConfig config,
                               ClanRepository clanRepository,
                               RosterService rosterService,
                               MessageFormatter formatter) {
        super(new CommandData(
                "clan-remove",
                config.getDescriptions().getClanRemove(),
                List.of(
                        new OptionData(OptionType.USER, "user", "User", true)
                )
        ));
        this.clanRepository = clanRepository;
        this.rosterService = rosterService;
        this.commandConfig = config;
        this.formatter = formatter;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getOption("user").getAsMember();

        boolean exists = clanRepository.getStorage().getMembers()
                .stream().anyMatch(m -> m.getUserId().equals(member.getId()));

        String template;
        if (exists) {
            clanRepository.removeMember(member.getId());
            rosterService.updateRosterMessage();
            template = commandConfig.getMessages().getClanRemoveSuccess();
        } else {
            template = commandConfig.getMessages().getClanRemoveNotFound();
        }

        String reply = formatter.format(template, "user", member.getAsMention());
        event.reply(reply).setEphemeral(true).queue();
    }
}