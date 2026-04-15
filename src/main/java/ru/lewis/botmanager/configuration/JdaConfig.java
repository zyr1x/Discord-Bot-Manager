package ru.lewis.botmanager.configuration;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("discord")
@Getter
@Setter
public class JdaConfig {
    private String token;
    private String guildId;

    @Bean
    public JDA jda() {
        try {
            return JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .build()
                    .awaitReady();
        } catch (InterruptedException exception) {
            throw new NullPointerException();
        }
    }
}
