package ru.lewis.botmanager.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class LogRepository {

    private static final String FILE_PATH = "log_data.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private LogStorage storage;

    @PostConstruct
    public void load() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try { storage = mapper.readValue(file, LogStorage.class); }
            catch (IOException e) { storage = new LogStorage(); }
        } else {
            storage = new LogStorage();
        }
    }

    public void save() {
        try { mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), storage); }
        catch (IOException e) { throw new RuntimeException("Не удалось сохранить log_data.json", e); }
    }

    public String getChannelId() { return storage.getChannelId(); }

    public void setChannelId(String channelId) {
        storage.setChannelId(channelId);
        save();
    }

    @Data
    @NoArgsConstructor
    public static class LogStorage {
        private String channelId;
    }
}