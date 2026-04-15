package ru.lewis.botmanager.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;
import ru.lewis.botmanager.configuration.CommandConfig;
import ru.lewis.botmanager.configuration.JdaConfig;
import ru.lewis.botmanager.model.ClanMember;
import ru.lewis.botmanager.model.ClanStorage;
import ru.lewis.botmanager.repository.ClanRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RosterService {

    private final ClanRepository clanRepository;
    private final JDA jda;
    private final JdaConfig jdaConfig;
    private final CommandConfig commandConfig;

    public RosterService(ClanRepository clanRepository,
                         JDA jda,
                         JdaConfig jdaConfig,
                         CommandConfig commandConfig) {
        this.clanRepository = clanRepository;
        this.jda = jda;
        this.jdaConfig = jdaConfig;
        this.commandConfig = commandConfig;
    }

    public void cleanupAsync(Guild guild, Runnable onComplete) {
        ClanStorage storage = clanRepository.getStorage();

        List<ClanMember> members = new ArrayList<>(storage.getMembers());
        List<ClanMember> toRemove = new ArrayList<>();

        guild.getMemberById()

        if (members.isEmpty()) {
            onComplete.run();
            return;
        }

        final int[] remaining = {members.size()};

        for (ClanMember member : members) {
            Role role = guild.getRoleById(member.getRoleId());
            if (role == null) {
                toRemove.add(member);
                if (--remaining[0] == 0) finishCleanup(storage, toRemove, onComplete);
                continue;
            }

            guild.retrieveMemberById(member.getUserId()).queue(
                    m -> {
                        if (!m.getRoles().contains(role)) {
                            toRemove.add(member);
                        }

                        if (--remaining[0] == 0) {
                            finishCleanup(storage, toRemove, onComplete);
                        }
                    },
                    error -> {
                        toRemove.add(member);

                        if (--remaining[0] == 0) {
                            finishCleanup(storage, toRemove, onComplete);
                        }
                    }
            );
        }
    }

    private void finishCleanup(ClanStorage storage, List<ClanMember> toRemove, Runnable onComplete) {
        if (!toRemove.isEmpty()) {
            storage.getMembers().removeAll(toRemove);
            clanRepository.save();
        }

        onComplete.run();
    }

    public void updateRosterMessage() {
        ClanStorage storage = clanRepository.getStorage();
        if (storage.getRosterMessageId() == null || storage.getRosterChannelId() == null) return;

        Guild guild = jda.getGuildById(jdaConfig.getGuildId());
        if (guild == null) return;

        TextChannel channel = guild.getTextChannelById(storage.getRosterChannelId());
        if (channel == null) return;

        cleanupAsync(guild, () -> {
            String content = buildRosterContent(guild, storage);

            channel.retrieveMessageById(storage.getRosterMessageId()).queue(
                    message -> message.editMessage(content).queue(),
                    error -> channel.sendMessage(content).queue(newMessage ->
                            clanRepository.setRosterMessage(channel.getId(), newMessage.getId())
                    )
            );
        });
    }

    public String buildRosterContent(Guild guild, ClanStorage storage) {
        CommandConfig.Roster cfg = commandConfig.getRoster();
        CommandConfig.Messages msg = commandConfig.getMessages();

        if (storage.getMembers().isEmpty()) {
            return msg.getClanEmpty();
        }

        Map<String, List<String>> byRole = new LinkedHashMap<>();
        for (ClanMember member : storage.getMembers()) {
            Role role = guild.getRoleById(member.getRoleId());
            String roleName = role != null ? role.getName() : "❓ unknown role";
            byRole.computeIfAbsent(roleName, k -> new ArrayList<>())
                    .add(member.getUserId());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(cfg.getTitle()).append("\n");
        sb.append(cfg.getSeparator()).append("\n\n");

        for (Map.Entry<String, List<String>> entry : byRole.entrySet()) {
            sb.append(cfg.getRoleHeader().replace("{role}", entry.getKey())).append("\n");
            for (String userId : entry.getValue()) {
                sb.append(cfg.getMemberLine().replace("{user}", "<@" + userId + ">")).append("\n");
            }
            sb.append("\n");
        }

        sb.append(cfg.getSeparator()).append("\n");
        sb.append(cfg.getFooter().replace("{count}", String.valueOf(storage.getMembers().size())));

        return sb.toString();
    }
}