package ru.lewis.botmanager.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.model.ClanMember;
import ru.lewis.botmanager.model.ClanStorage;

import java.io.File;
import java.io.IOException;

@Component
public class ClanRepository {

    private static final String FILE_PATH = "clan_data.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ClanStorage storage;

    @PostConstruct
    public void load() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try {
                storage = objectMapper.readValue(file, ClanStorage.class);
            } catch (IOException e) {
                storage = new ClanStorage();
            }
        } else {
            storage = new ClanStorage();
        }
    }

    public void save() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), storage);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить clan_data.json", e);
        }
    }

    public ClanStorage getStorage() {
        return storage;
    }

    public void addMember(String userId, String roleId) {
        storage.getMembers().removeIf(m -> m.getUserId().equals(userId));
        storage.getMembers().add(new ClanMember(userId, roleId));
        save();
    }

    public void removeMember(String userId) {
        storage.getMembers().removeIf(m -> m.getUserId().equals(userId));
        save();
    }

    public void setRosterMessage(String channelId, String messageId) {
        storage.setRosterChannelId(channelId);
        storage.setRosterMessageId(messageId);
        save();
    }
}
