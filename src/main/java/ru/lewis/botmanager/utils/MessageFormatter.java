package ru.lewis.botmanager.utils;

import org.springframework.stereotype.Component;

@Component
public class MessageFormatter {

    public String format(String template, Object... keyValues) {
        String result = template;
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            result = result.replace("{" + keyValues[i] + "}", String.valueOf(keyValues[i + 1]));
        }
        return result;
    }
}
