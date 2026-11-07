package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.i18n.Names;
import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.save.SaveData;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of the cosmetic shop: who owns what, what coins can buy, what gets equipped.
 *
 * <p>Pure: every method reads and writes the {@link SaveData} it is given and nothing else.
 * Screens call these and draw the result; they never decide on their own whether a purchase
 * is allowed. Coins are debited exactly once, inside {@link #buy}, and only when the item is
 * for sale, not yet owned and affordable.</p>
 */
public final class Shop {

    /** Outcome of {@link #buy}. */
    public enum PurchaseResult {
        BOUGHT, ALREADY_OWNED, NOT_FOR_SALE, NOT_ENOUGH_COINS
    }

    private Shop() {
    }

    // ---------------------------------------------------------------- ownership
    /**
     * @return {@code true} for free items, for progression unlocks whose condition is met, and
     *         for everything the player has bought
     */
    public static boolean isOwned(SaveData save, Cosmetic item) {
        switch (item.unlock()) {
            case DAILY_STREAK:
                return save.dailyPaletteUnlocked;
            case STARS:
                return StarRoad.isUnlocked(item, save.totalStars());
            case CHAPTER:
                return chapterCleared(save, item.requirement());
            default:
                return item.price() == 0 || save.ownsCosmetic(item.id());
        }
    }

    /** Shop grid filters: 100 items a tab need a way to find what matters now. */
    public enum Filter {
        ALL(T.k("ALL")), OWNED(T.k("OWNED")), COINS(T.k("FOR COINS")), EARN(T.k("TO EARN"));

        private final String label;

        Filter(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * @return the items matching the filter, in catalogue order, except {@link Filter#COINS}
     *         which lists what the purse can afford first, then by price
     */
    public static List<Cosmetic> filter(SaveData save, List<Cosmetic> items, Filter filter) {
        List<Cosmetic> out = new ArrayList<>();
        for (Cosmetic item : items) {
            boolean owned = isOwned(save, item);
            boolean keep;
            switch (filter) {
                case OWNED:
                    keep = owned;
                    break;
                case COINS:
                    keep = !owned && item.isForSale();
                    break;
                case EARN:
                    keep = !owned && item.unlock() != Cosmetic.Unlock.NONE;
                    break;
                default:
                    keep = true;
                    break;
            }
            if (keep) out.add(item);
        }
        if (filter == Filter.EARN) {
            // Nearest goals first: star skins by stars needed, then chapter rewards, then others.
            out.sort((a, b) -> {
                int typeA = a.unlock() == Cosmetic.Unlock.STARS ? 0 : a.unlock() == Cosmetic.Unlock.CHAPTER ? 1 : 2;
                int typeB = b.unlock() == Cosmetic.Unlock.STARS ? 0 : b.unlock() == Cosmetic.Unlock.CHAPTER ? 1 : 2;
                if (typeA != typeB) return Integer.compare(typeA, typeB);
                return Integer.compare(a.requirement(), b.requirement());
            });
        }
        if (filter == Filter.COINS) {
            out.sort((a, b) -> {
                boolean affordA = save.coins >= a.price();
                boolean affordB = save.coins >= b.price();
                if (affordA != affordB) return affordA ? -1 : 1;
                return Integer.compare(a.price(), b.price());
            });
        }
        return out;
    }

    /** @return true once every level of that chapter is cleared (the next one is unlocked) */
    public static boolean chapterCleared(SaveData save, int chapter) {
        return chapter >= 1 && save.highestUnlocked > World.biomeOfChapter(chapter).lastLevel;
    }

    /**
     * Progression items (star road, chapter rewards, daily streak) the player owns right now:
     * snapshot before a win, compare after, and announce the difference.
     */
    public static List<Cosmetic> ownedProgressionItems(SaveData save) {
        List<Cosmetic> items = new ArrayList<>();
        for (Cosmetic item : Cosmetic.ALL) {
            if (item.unlock() != Cosmetic.Unlock.NONE && isOwned(save, item)) {
                items.add(item);
            }
        }
        return items;
    }

    /** "42 / 60 stars" for a locked star-road item; empty for anything else or once owned. */
    public static String starProgress(SaveData save, Cosmetic item) {
        int required = StarRoad.starsRequired(item);
        if (required < 0 || isOwned(save, item)) {
            return "";
        }
        return T.rawf("{0} / {1} stars", Math.min(save.totalStars(), required), required);
    }

    /** "42 / 60" for a locked star-road item; empty for anything else or once owned. */
    public static String starCount(SaveData save, Cosmetic item) {
        int required = StarRoad.starsRequired(item);
        if (required < 0 || isOwned(save, item)) {
            return "";
        }
        return Math.min(save.totalStars(), required) + " / " + required;
    }

    /** Text explaining how a locked progression item is earned; empty for shop items. */
    public static String unlockHint(Cosmetic item) {
        switch (item.unlock()) {
            case DAILY_STREAK:
                return T.rawf("Win {0} daily puzzles in a row", GameConfig.DAILY_PALETTE_STREAK);
            case STARS:
                return T.rawf("Collect {0} campaign stars", StarRoad.starsRequired(item));
            case CHAPTER:
                return T.rawf("Clear chapter {0}, {1}", item.requirement(),
                    Names.name(World.biomeOfChapter(item.requirement()).name));
            default:
                return "";
        }
    }

    public static boolean canBuy(SaveData save, Cosmetic item) {
        return item.isForSale() && !isOwned(save, item) && save.coins >= item.price();
    }

    // ----------------------------------------------------------------- buying
    /**
     * Buys and equips the item. Coins move only on {@link PurchaseResult#BOUGHT}.
     */
    public static PurchaseResult buy(SaveData save, Cosmetic item) {
        if (isOwned(save, item)) {
            return PurchaseResult.ALREADY_OWNED;
        }
        if (!item.isForSale()) {
            return PurchaseResult.NOT_FOR_SALE;
        }
        if (!save.spendCoins(item.price())) {
            return PurchaseResult.NOT_ENOUGH_COINS;
        }
        save.addOwnedCosmetic(item.id());
        save.setEquippedCosmetic(item.category(), item.id());
        return PurchaseResult.BOUGHT;
    }

    // --------------------------------------------------------------- equipping
    /** @return {@code true} when the item is owned and is now the one in use */
    public static boolean equip(SaveData save, Cosmetic item) {
        if (!isOwned(save, item)) {
            return false;
        }
        save.setEquippedCosmetic(item.category(), item.id());
        return true;
    }

    /**
     * The item in use for a category. An unknown id (removed item, corrupt save) or an id the
     * player does not own any more (progress reset) falls back to the category's free item.
     */
    public static Cosmetic equipped(SaveData save, Cosmetic.Category category) {
        Cosmetic item = Cosmetic.byId(save.equippedCosmetic(category));
        if (item == null || item.category() != category || !isOwned(save, item)) {
            return Cosmetic.defaultFor(category);
        }
        return item;
    }

    public static boolean isEquipped(SaveData save, Cosmetic item) {
        return equipped(save, item.category()) == item;
    }

    // ------------------------------------------------------------ discovery
    /**
     * Shop items the player can now afford and has not been told about yet. Each is announced
     * once: callers mark it with {@link SaveData#markShopNotified} when they show it.
     */
    public static List<Cosmetic> newlyAffordable(SaveData save) {
        List<Cosmetic> items = new ArrayList<>();
        for (Cosmetic item : Cosmetic.ALL) {
            if (canBuy(save, item) && !save.isShopNotified(item.id())) {
                items.add(item);
            }
        }
        return items;
    }

    /** @return how many shop items the player can afford right now and does not own */
    public static int affordableCount(SaveData save) {
        int count = 0;
        for (Cosmetic item : Cosmetic.ALL) {
            if (canBuy(save, item)) count++;
        }
        return count;
    }

    /** @return owned items across all categories, free defaults included */
    public static int ownedCount(SaveData save) {
        int count = 0;
        for (Cosmetic item : Cosmetic.ALL) {
            if (isOwned(save, item)) count++;
        }
        return count;
    }
}
