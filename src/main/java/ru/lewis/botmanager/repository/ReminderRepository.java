package ru.lewis.botmanager.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.lewis.botmanager.model.ReminderModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ReminderRepository {

    private static final String FILE_PATH = "reminders_data.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private ReminderStorage storage;

    @PostConstruct
    public void load() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try { storage = mapper.readValue(file, ReminderStorage.class); }
            catch (IOException e) { storage = new ReminderStorage(); }
        } else {
            storage = new ReminderStorage();
        }
    }

    public void save() {
        try { mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), storage); }
        catch (IOException e) { throw new RuntimeException("Не удалось сохранить reminders_data.json", e); }
    }

    public void add(ReminderModel reminder) {
        storage.getReminders().add(reminder);
        save();
    }

    public boolean remove(String userId, long reminderId) {
        boolean removed = storage.getReminders()
                .removeIf(r -> r.getId() == reminderId && r.getUserId().equals(userId));
        if (removed) save();
        return removed;
    }

    public void removeById(long reminderId) {
        storage.getReminders().removeIf(r -> r.getId() == reminderId);
        save();
    }

    public List<ReminderModel> getByUser(String userId) {
        return storage.getReminders().stream()
                .filter(r -> r.getUserId().equals(userId))
                .toList();
    }

    /** Все напоминания которые уже пора исполнить */
    public List<ReminderModel> getDue(long nowMillis) {
        return storage.getReminders().stream()
                .filter(r -> r.getFireAtMillis() <= nowMillis)
                .toList();
    }

    public List<ReminderModel> getAll() { return storage.getReminders(); }

    @Data
    @NoArgsConstructor
    public static class ReminderStorage {
        private List<ReminderModel> reminders = new ArrayList<>();
    }
}