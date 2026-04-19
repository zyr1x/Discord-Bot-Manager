package ru.lewis.botmanager.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.configuration.CommandConfig;
import ru.lewis.botmanager.model.CommandData;
import ru.lewis.botmanager.model.CommandExecutor;
import ru.lewis.botmanager.repository.ClanRepository;
import ru.lewis.botmanager.service.LogService;
import ru.lewis.botmanager.service.RosterService;
import ru.lewis.botmanager.utils.MessageFormatter;

import java.util.List;

@Component
public class AddMemberCommand extends CommandExecutor {

    private final ClanRepository clanRepository;
    private final RosterService rosterService;
    private final CommandConfig commandConfig;
    private final MessageFormatter formatter;
    private final LogService logService;

    public AddMemberCommand(CommandConfig config,
                            ClanRepository clanRepository,
                            RosterService rosterService,
                            MessageFormatter formatter,
                            LogService logService) {
        super(new CommandData(
                "clan-add",
                config.getDescriptions().getClanAdd(),
                List.of(
                        new OptionData(OptionType.USER, "user", "User", true),
                        new OptionData(OptionType.ROLE, "role", "Role in clan", true)
                )
        ));
        this.clanRepository = clanRepository;
        this.rosterService = rosterService;
        this.commandConfig = config;
        this.formatter = formatter;
        this.logService = logService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getOption("user").getAsMember();
        Role role = event.getOption("role").getAsRole();

        boolean exists = clanRepository.getStorage().getMembers()
                .stream().anyMatch(m -> m.getUserId().equals(member.getId()));

        clanRepository.addMember(member.getId(), role.getId());
        rosterService.updateRosterMessage();

        String template = exists
                ? commandConfig.getMessages().getClanAddReplace()
                : commandConfig.getMessages().getClanAddSuccess();

        String reply = formatter.format(template,
                "user", member.getAsMention(),
                "role", role.getName());

        event.reply(reply).setEphemeral(true).queue();

        logService.log(
                LogService.Type.CLAN_ADD,
                event.getMember().getAsMention(),
                member.getAsMention(),
                "Роль: " + role.getName()
        );
    }
}