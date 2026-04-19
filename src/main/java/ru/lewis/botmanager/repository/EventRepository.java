package ru.lewis.botmanager.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.model.EventModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class EventRepository {

    private static final String FILE_PATH = "events_data.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private EventStorage storage;

    @PostConstruct
    public void load() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try { storage = mapper.readValue(file, EventStorage.class); }
            catch (IOException e) { storage = new EventStorage(); }
        } else {
            storage = new EventStorage();
        }
    }

    public void save() {
        try { mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), storage); }
        catch (IOException e) { throw new RuntimeException("Не удалось сохранить events_data.json", e); }
    }

    public Optional<EventModel> findById(String messageId) {
        return storage.getEvents().stream()
                .filter(e -> e.getId().equals(messageId))
                .findFirst();
    }

    public void add(EventModel event) {
        storage.getEvents().add(event);
        save();
    }

    public void update(EventModel event) {
        storage.getEvents().removeIf(e -> e.getId().equals(event.getId()));
        storage.getEvents().add(event);
        save();
    }

    public void remove(String messageId) {
        storage.getEvents().removeIf(e -> e.getId().equals(messageId));
        save();
    }

    public List<EventModel> getAll() { return storage.getEvents(); }

    @Data
    @NoArgsConstructor
    public static class EventStorage {
        private List<EventModel> events = new ArrayList<>();
    }
}