package ru.lewis.botmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class CommandData {
    private String name;
    private String description;
    private List<OptionData> options;

    public CommandData(String name, String description) {
        this.name = name;
        this.description = description;
        this.options = new ArrayList<>();
    }
}
