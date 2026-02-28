package com.houdert6.ri2.data.itemtiers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import java.util.function.Function;

/**
 * A tier up requirement that accepts any item from a tag
 */
public class ItemTagTierUpRequirement extends TierUpRequirement {
    private final Tag<Material> tag;

    public ItemTagTierUpRequirement(Tag<Material> tag) {
        this.tag = tag;
    }
    @Override
    public boolean matches(Material type) {
        return tag.isTagged(type);
    }
    @Override
    public String key() {
        // If the item tag is in the minecraft: namespace it's just returned as #itemtag, otherwise it's returned as #namespace:itemtag
        return "#" + (tag.getKey().getNamespace().equals(NamespacedKey.MINECRAFT) ? tag.getKey().getKey() : tag.getKey().getNamespace() + ":" + tag.getKey().getKey());
    }
    @Override
    public Component fillInPlaceholder(String template, Function<String, String> preprocessor, String placeholder) {
        // For example, if this tag is "minecraft:pickaxes" then the placeholder is replaced with "pickaxes"
        template = template.replace(placeholder, tag.getKey().getKey());
        return LegacyComponentSerializer.legacyAmpersand().deserialize(preprocessor == null ? template : preprocessor.apply(template));
    }
}
