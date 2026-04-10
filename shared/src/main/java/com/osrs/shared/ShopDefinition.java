package com.osrs.shared;

import java.util.List;

public final class ShopDefinition {
    public final String id;
    public final String name;
    public final String npcName;
    public final List<StockEntry> stock;

    public ShopDefinition(String id, String name, String npcName, List<StockEntry> stock) {
        this.id = id;
        this.name = name;
        this.npcName = npcName;
        this.stock = stock;
    }

    public static final class StockEntry {
        public final int itemId;
        public final int quantity;
        public final Integer price;

        public StockEntry(int itemId, int quantity, Integer price) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
