package ru.lewis.botmanager.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.springframework.stereotype.Service;
import ru.lewis.botmanager.configuration.CommandConfig;

import java.awt.*;
import java.util.*;
import java.util.List;

@Service
public class EmbedService extends ListenerAdapter {

    private static final String SELECT_ID = "embed:roles";
    private static final String BUTTON_ID = "embed:next";
    private static final String MODAL_ID  = "embed:modal";

    private static final String TITLE_ID = "title";
    private static final String DESC_ID  = "desc";
    private static final String COLOR_ID = "color";
    private static final String IMAGE_ID = "image";
    private static final String FOOTER_ID = "footer";

    private final Map<String, List<String>> roleCache = new HashMap<>();
    private final CommandConfig config;

    public EmbedService(JDA jda, CommandConfig config) {
        this.config = config;
        jda.addEventListener(this);
    }

    public void start(SlashCommandInteractionEvent event) {

        List<Role> roles = event.getGuild().getRoles().stream()
                .filter(r -> !r.isManaged())
                .filter(r -> !r.isPublicRole())
                .filter(r -> event.getGuild().getSelfMember().canInteract(r))
                .limit(25)
                .toList();

        StringSelectMenu menu = StringSelectMenu.create(SELECT_ID)
                .setPlaceholder(config.getEmbed().getSelectPlaceholder())
                .setMinValues(0)
                .setMaxValues(Math.min(roles.size(), 25))
                .addOptions(
                        roles.stream()
                                .map(role -> SelectOption.of(role.getName(), role.getId()))
                                .toList()
                )
                .build();

        var button = Button.primary(
                BUTTON_ID,
                config.getEmbed().getNextButton()
        );

        event.reply(config.getMessages().getEmbedRoleSelect())
                .setComponents(
                        ActionRow.of(menu),
                        ActionRow.of(button)
                )
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals(SELECT_ID)) return;

        List<String> roles = event.getValues();

        roleCache.put(event.getUser().getId(), roles);

        event.deferEdit().queue(hook -> {});
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        if (!event.getComponentId().equals(SELECT_ID)) return;

        List<String> roles = new ArrayList<>();

        event.getMentions().getRoles().forEach(role -> roles.add(role.getId()));

        roleCache.put(event.getUser().getId(), roles);

        event.deferEdit().queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals(BUTTON_ID)) return;

        Modal modal = Modal.create(MODAL_ID, config.getEmbed().getModalTitle())
                .addComponents(
                        Label.of(config.getEmbed().getTitleInput(),
                                TextInput.create(TITLE_ID, TextInputStyle.SHORT).setRequired(false).build()),

                        Label.of(config.getEmbed().getDescInput(),
                                TextInput.create(DESC_ID, TextInputStyle.PARAGRAPH).setRequired(true).build()),

                        Label.of(config.getEmbed().getColorInput(),
                                TextInput.create(COLOR_ID, TextInputStyle.SHORT).setRequired(false).build()),

                        Label.of(config.getEmbed().getImageInput(),
                                TextInput.create(IMAGE_ID, TextInputStyle.SHORT).setRequired(false).build()),

                        Label.of(config.getEmbed().getFooterInput(),
                                TextInput.create(FOOTER_ID, TextInputStyle.SHORT).setRequired(false).build())
                )
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals(MODAL_ID)) return;

        String userId = event.getUser().getId();
        List<String> roles = roleCache.remove(userId);

        String title  = get(event, TITLE_ID);
        String desc   = get(event, DESC_ID);
        String color  = get(event, COLOR_ID);
        String image  = get(event, IMAGE_ID);
        String footer = get(event, FOOTER_ID);

        EmbedBuilder eb = new EmbedBuilder();

        eb.setDescription(desc);

        if (title != null) eb.setTitle(title);
        eb.setColor(color == null
                ? Color.decode(config.getEmbed().getDefaultColor())
                : parseColor(color));

        if (image != null && image.startsWith("http")) {
            eb.setImage(image);
        }

        if (footer != null) {
            eb.setFooter(footer);
        }

        StringBuilder mentions = new StringBuilder();

        if (roles != null) {
            for (String roleId : roles) {
                mentions.append("<@&").append(roleId).append("> ");
            }
        }

        event.reply(mentions.toString())
                .setAllowedMentions(Collections.singletonList(Message.MentionType.ROLE))
                .addEmbeds(eb.build())
                .queue();
    }

    private String get(ModalInteractionEvent event, String id) {
        var val = event.getValue(id);
        if (val == null) return null;

        String s = val.getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    private Color parseColor(String raw) {
        if (raw == null) return Color.GRAY;

        try {
            return Color.decode(raw);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }
}