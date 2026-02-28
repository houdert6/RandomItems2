package com.houdert6.ri2.data.itemtiers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A tier up requirement which accepts a single Bukkit {@link Material}
 */
public class SingleMaterialTierUpRequirement extends TierUpRequirement {
    private final Material type;

    public SingleMaterialTierUpRequirement(Material type) {
        this.type = type;
    }
    @Override
    public boolean matches(Material type) {
        return type == this.type;
    }
    @Override
    public String key() {
        return type.name();
    }
    @Override
    public Component fillInPlaceholder(String template, Function<String, String> preprocessor, String placeholder) {
        return itemPlaceholder(template, preprocessor, placeholder, type);
    }
    private static Component styleLast(Component c, Component from, Style last) {
        Component lastMost = from;
        while (!lastMost.children().isEmpty()) {
            lastMost = lastMost.children().getLast();
        }
        if (last != null) {
            return c.style(last.merge(lastMost.style()));
        }
        return c.style(lastMost.style());
    }
    /**
     * Replaces all occurrences of the given placeholder with a translatable item name component, and returns the message as a {@link Component}
     * @param itemNameSplitPreprocessor called once per each part of the string split up by {@code placeholder}, mainly used for PlaceholderAPI integration. Can be null
     */
    public static Component itemPlaceholder(String template, Function<String, String> itemNameSplitPreprocessor, String placeholder, Material item) {
        String[] itemNameSplit = (template + " ").split(placeholder); // The space is added so split() adds an extra element to the end of the array if the template ends in the placeholder
        if (itemNameSplitPreprocessor != null) {
            for (int i = 0; i < itemNameSplit.length; i++) {
                itemNameSplit[i] = itemNameSplitPreprocessor.apply(itemNameSplit[i]);
            }
        }
        LegacyComponentSerializer deserializer = LegacyComponentSerializer.legacyAmpersand();
        Component actionBarComponent = deserializer.deserialize(itemNameSplit[0]);
        List<Component> actionBar = new ArrayList<>();
        actionBar.add(actionBarComponent);
        Style last = null;
        for (int i = 1; i < itemNameSplit.length; i++) {
            TextComponent text = deserializer.deserialize(itemNameSplit[i]);
            actionBarComponent = styleLast(Component.empty().append(Component.translatable(item)
                    .append(text)), actionBarComponent, last);
            last = actionBarComponent.style();
            actionBar.add(actionBarComponent);
        }
        return actionBar.stream().reduce(Component::append).orElse(Component.empty());
    }
}
