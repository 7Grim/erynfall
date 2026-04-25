package com.osrs.server.shop;

import com.osrs.shared.ShopDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Mutable runtime shop state. One instance per shop definition.
 * Thread-safe: mutation methods are synchronized for concurrent Netty/GameLoop access.
 */
public final class ShopStock {

    private final String shopId;
    private final String shopName;
    private final List<Integer> orderedItemIds;
    private final Map<Integer, Integer> baseQuantities;
    private final Map<Integer, Integer> basePrices;
    private final Map<Integer, Integer> currentQuantities;

    public ShopStock(ShopDefinition def, IntUnaryOperator fallbackPrice) {
        this.shopId   = def.id;
        this.shopName = def.name;
        orderedItemIds    = new ArrayList<>();
        baseQuantities    = new LinkedHashMap<>();
        basePrices        = new LinkedHashMap<>();
        currentQuantities = new LinkedHashMap<>();

        for (ShopDefinition.StockEntry e : def.stock) {
            int qty   = Math.max(0, e.quantity);
            int price = (e.price != null && e.price > 0) ? e.price : Math.max(1, fallbackPrice.applyAsInt(e.itemId));
            orderedItemIds.add(e.itemId);
            baseQuantities.put(e.itemId, qty);
            basePrices.put(e.itemId, price);
            currentQuantities.put(e.itemId, qty);
        }
    }

    public String getShopId()   { return shopId; }
    public String getShopName() { return shopName; }

    public List<Integer> getOrderedItemIds() {
        return Collections.unmodifiableList(orderedItemIds);
    }

    public boolean hasItem(int itemId) {
        return baseQuantities.containsKey(itemId);
    }

    public synchronized int getCurrentQuantity(int itemId) {
        return currentQuantities.getOrDefault(itemId, 0);
    }

    public int getBaseQuantity(int itemId) {
        return baseQuantities.getOrDefault(itemId, 0);
    }

    public int getBasePrice(int itemId) {
        return basePrices.getOrDefault(itemId, 1);
    }

    /**
     * Buy price (player buys from shop).
     * 1x at full stock, 2x when stock reaches 0. Linear.
     * Returns -1 if item not stocked here.
     */
    public int computeBuyPrice(int itemId) {
        if (!hasItem(itemId)) return -1;
        int base    = getBasePrice(itemId);
        int baseQty = getBaseQuantity(itemId);
        if (baseQty <= 0) return base;
        int current = getCurrentQuantity(itemId);
        double factor = 2.0 - (double) current / baseQty;
        return Math.max(1, (int) Math.round(base * Math.max(1.0, factor)));
    }

    /**
     * Sell price (player sells to shop).
     * 40% of base price at normal stock, drops to 10% when shop has excess.
     * Returns -1 if item not stocked here.
     */
    public int computeSellPrice(int itemId) {
        if (!hasItem(itemId)) return -1;
        int base    = getBasePrice(itemId);
        int baseQty = getBaseQuantity(itemId);
        int current = getCurrentQuantity(itemId);
        double excess  = baseQty > 0 ? (double) current / baseQty : 1.0;
        double factor  = Math.max(0.1, 0.4 * (2.0 - Math.max(1.0, excess)));
        return Math.max(1, (int) Math.floor(base * factor));
    }

    /**
     * Attempt to decrement stock for a purchase. Returns actual amount decremented.
     */
    public synchronized int tryDecrement(int itemId, int requested) {
        int current = currentQuantities.getOrDefault(itemId, 0);
        if (current <= 0) return 0;
        int actual = Math.min(requested, current);
        currentQuantities.put(itemId, current - actual);
        return actual;
    }

    /**
     * Increment stock when a player sells to this shop.
     */
    public synchronized void increment(int itemId, int amount) {
        if (!hasItem(itemId) || amount <= 0) return;
        currentQuantities.merge(itemId, amount, Integer::sum);
    }

    /**
     * Restock tick: move each item one step toward its base quantity.
     */
    public synchronized void restock() {
        for (int itemId : baseQuantities.keySet()) {
            int base    = baseQuantities.get(itemId);
            int current = currentQuantities.getOrDefault(itemId, 0);
            if (current < base) {
                currentQuantities.put(itemId, current + 1);
            } else if (current > base) {
                currentQuantities.put(itemId, current - 1);
            }
        }
    }
}
