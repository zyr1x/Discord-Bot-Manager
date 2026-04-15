package ru.lewis.botmanager.listener;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.repository.ClanRepository;
import ru.lewis.botmanager.service.RosterService;

@Component
public class GuildMemberLeaveListener extends ListenerAdapter {

    private final ClanRepository clanRepository;
    private final RosterService rosterService;

    public GuildMemberLeaveListener(JDA jda,
                                    ClanRepository clanRepository,
                                    RosterService rosterService) {
        this.clanRepository = clanRepository;
        this.rosterService = rosterService;
        jda.addEventListener(this);
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        String userId = event.getUser().getId();
        boolean wasMember = clanRepository.getStorage().getMembers()
                .stream().anyMatch(m -> m.getUserId().equals(userId));

        if (wasMember) {
            clanRepository.removeMember(userId);
            rosterService.updateRosterMessage();
        }
    }
}