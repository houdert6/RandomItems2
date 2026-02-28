package com.houdert6.ri2.data.itemtiers;

import java.util.*;
import java.util.function.Function;

/**
 * A weighted list implementation very loosely based off the net.minecraft version
 */
public class WeightedList<T> extends AbstractList<T> {
    /**
     * Weighted items contained in this list
     */
    private final List<WeightedItem<T>> items;
    /**
     * Total weight of all items
     */
    private final int totalWeight;
    /**
     * Function used to select a weighted item at a given index
     */
    private final Function<Integer, T> selector;

    private WeightedList(List<WeightedItem<T>> items) {
        this.items = items;
        if (items.isEmpty()) {
            selector = index -> null;
            totalWeight = 0;
        } else {
            int weightCount = 0;
            for (WeightedItem<T> item : items) {
                weightCount += item.weight;
            }
            totalWeight = weightCount;
            if (totalWeight < 64) {
                // "flat" weighted list implementation
                // An object array is used and directly accessed which contains
                Object[] flat = makeFlatArray();
                // Selector selects the item from the flat array
                selector = index -> (T)flat[index];
            } else {
                // Use compact selection
                selector = this::selectCompact;
            }
        }
    }

    @Override
    public T get(int index) {
        return selector.apply(index);
    }
    @Override
    public int size() {
        return totalWeight;
    }

    // Selection methods

    /**
     * The "flat" method. Each item is added to an array as many times as its weight, and items are directly selected from the array
     * @return The "flat" array used for this selection method
     * @see net.minecraft.util.random.WeightedList.Flat
     */
    private Object[] makeFlatArray() {
        Object[] flat = new Object[totalWeight];
        int i = 0;
        for (WeightedItem<T> item : items) {
            // Add the item to the flat[] `item.weight` times
            Arrays.fill(flat, i, i += item.weight, item.item);
        }
        return flat;
    }

    /**
     * The "compact" method. The list of weighted items is iterated, and the weight of each item is subtracted from the given index until the given index < 0, at which point the current item is returned
     * @return The item at the given index
     * @see net.minecraft.util.random.WeightedList.Compact
     */
    private T selectCompact(int index) {
        Objects.checkIndex(index, totalWeight); // Make sure the index is in-bounds
        for (WeightedItem<T> item : items) {
            // If the index, with the item's weight subtracted from it, is less than 0, return this item
            if ((index -= item.weight) < 0) {
                return item.item;
            }
        }
        throw new IllegalStateException(index + " exceeded total weight");
    }

    // Utility Classes

    /**
     * Builder for a weighted list that accepts weighted items
     */
    public static class Builder<T> extends ArrayList<WeightedItem<T>> {
        /**
         * Adds an item with the specified weight
         * @param item item to add
         * @param weight weight of the item
         */
        public void add(T item, int weight) {
            this.add(new WeightedItem<>(item, weight));
        }

        public WeightedList<T> build() {
            return new WeightedList<>(this);
        }
    }

    /**
     * An item with an associated weight
     * @param item the item
     * @param weight weight of the item
     */
    public record WeightedItem<T>(T item, int weight) {}
}
