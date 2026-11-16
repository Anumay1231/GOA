package com.example.gap;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The products the shop sells, with current prices. */
final class Catalog {

    private static final List<GroceryItem> ITEMS = Collections.unmodifiableList(build());

    private Catalog() {}

    static List<GroceryItem> items() {
        return ITEMS;
    }

    /** Finds a product by its exact name, or null if it's no longer sold. */
    @Nullable
    static GroceryItem find(String name) {
        for (GroceryItem item : ITEMS) {
            if (item.getName().equals(name)) return item;
        }
        return null;
    }

    private static List<GroceryItem> build() {
        List<GroceryItem> list = new ArrayList<>();
        list.add(new GroceryItem("Potato", 25, "kg", R.drawable.potato));
        list.add(new GroceryItem("Onion", 35, "kg", R.drawable.onion));
        list.add(new GroceryItem("Tomato", 40, "kg", R.drawable.tomato));
        list.add(new GroceryItem("Cucumber", 30, "kg", R.drawable.cucumber));
        list.add(new GroceryItem("Radish", 25, "kg", R.drawable.radish));
        list.add(new GroceryItem("Carrot", 40, "kg", R.drawable.carrot));
        list.add(new GroceryItem("Beetroot", 35, "kg", R.drawable.beetroot));
        list.add(new GroceryItem("Cabbage", 30, "kg", R.drawable.cabbage));
        list.add(new GroceryItem("Cauliflower", 45, "kg", R.drawable.cauliflower));
        list.add(new GroceryItem("Capsicum (Green)", 60, "kg", R.drawable.capsicum));
        list.add(new GroceryItem("Capsicum (Red)", 120, "kg", R.drawable.capsicum_red));
        list.add(new GroceryItem("Capsicum (Red & Yellow)", 240, "kg", R.drawable.capsicum_red_and_capsicum_yellow));
        list.add(new GroceryItem("Bitter Gourd", 50, "kg", R.drawable.bittergourd));
        list.add(new GroceryItem("Bottle Gourd", 35, "kg", R.drawable.bottlegourd));
        list.add(new GroceryItem("Snake Gourd", 40, "kg", R.drawable.snakegourd));
        list.add(new GroceryItem("Ridge Gourd", 45, "kg", R.drawable.ridgegourd));
        list.add(new GroceryItem("Brinjal (Eggplant)", 40, "kg", R.drawable.brinjal));
        list.add(new GroceryItem("Drumstick", 90, "kg", R.drawable.drumstick));
        list.add(new GroceryItem("Okra (Ladies Finger)", 50, "kg", R.drawable.okra));
        list.add(new GroceryItem("Spinach", 30, "kg", R.drawable.spinach));
        list.add(new GroceryItem("Coriander", 15, "bunch", R.drawable.coriander));
        list.add(new GroceryItem("Mint", 20, "bunch", R.drawable.mint));
        list.add(new GroceryItem("Spring Onion", 40, "kg", R.drawable.springonion));
        list.add(new GroceryItem("Green Peas", 100, "kg", R.drawable.greenpeas));
        list.add(new GroceryItem("Sweet Corn", 60, "kg", R.drawable.sweetcorn));
        list.add(new GroceryItem("Broccoli", 120, "kg", R.drawable.broccoli));
        list.add(new GroceryItem("Zucchini (Green)", 130, "kg", R.drawable.zucchini));
        list.add(new GroceryItem("Zucchini (Yellow)", 140, "kg", R.drawable.zucchini_yellow));
        list.add(new GroceryItem("Mushroom", 160, "kg", R.drawable.mushroom));
        list.add(new GroceryItem("Pumpkin", 30, "kg", R.drawable.pumpkin));
        list.add(new GroceryItem("Turnip", 40, "kg", R.drawable.turnip));
        list.add(new GroceryItem("Sweet Potato", 50, "kg", R.drawable.sweetpotato));
        list.add(new GroceryItem("Fenugreek (Methi)", 25, "kg", R.drawable.fenugreek));
        list.add(new GroceryItem("Curry Leaves", 10, "bunch", R.drawable.curryleaves));
        list.add(new GroceryItem("Green Chilli", 60, "kg", R.drawable.greenchilli));
        list.add(new GroceryItem("Red Chilli (Dry)", 180, "kg", R.drawable.redchilli));
        list.add(new GroceryItem("Garlic", 200, "kg", R.drawable.garlic));
        list.add(new GroceryItem("Ginger", 150, "kg", R.drawable.ginger));
        list.add(new GroceryItem("Lemon", 5, "piece", R.drawable.lemon));
        list.add(new GroceryItem("Celery", 120, "kg", R.drawable.celery));
        return list;
    }
}
