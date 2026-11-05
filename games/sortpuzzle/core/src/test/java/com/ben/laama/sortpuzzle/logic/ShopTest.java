package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.save.SaveData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Shop rules")
class ShopTest {

    private static SaveData saveWith(int coins) {
        SaveData save = new SaveData();
        save.addCoins(coins);
        return save;
    }

    @Test
    @DisplayName("free items are owned and equipped from the start")
    void defaultsAreOwned() {
        SaveData save = new SaveData();
        for (Cosmetic.Category category : Cosmetic.Category.values()) {
            Cosmetic free = Cosmetic.defaultFor(category);
            assertTrue(Shop.isOwned(save, free), free.id());
            assertEquals(free, Shop.equipped(save, category));
            assertTrue(Shop.isEquipped(save, free));
        }
        assertEquals(4, Shop.ownedCount(save));
    }

    @Test
    @DisplayName("a purchase debits the price exactly once and equips the item")
    void buyDebitsOnceAndEquips() {
        SaveData save = saveWith(600);
        assertEquals(Shop.PurchaseResult.BOUGHT, Shop.buy(save, Cosmetic.BALL_CANDY));
        assertEquals(600 - Cosmetic.BALL_CANDY.price(), save.coins);
        assertTrue(Shop.isOwned(save, Cosmetic.BALL_CANDY));
        assertEquals(Cosmetic.BALL_CANDY, Shop.equipped(save, Cosmetic.Category.BALL_PALETTE));

        assertEquals(Shop.PurchaseResult.ALREADY_OWNED, Shop.buy(save, Cosmetic.BALL_CANDY));
        assertEquals(600 - Cosmetic.BALL_CANDY.price(), save.coins);
    }

    @Test
    @DisplayName("not enough coins: refused, balance untouched, nothing equipped")
    void insufficientCoins() {
        SaveData save = saveWith(Cosmetic.THEME_SAKURA.price() - 1);
        assertFalse(Shop.canBuy(save, Cosmetic.THEME_SAKURA));
        assertEquals(Shop.PurchaseResult.NOT_ENOUGH_COINS, Shop.buy(save, Cosmetic.THEME_SAKURA));
        assertEquals(Cosmetic.THEME_SAKURA.price() - 1, save.coins);
        assertFalse(Shop.isOwned(save, Cosmetic.THEME_SAKURA));
        assertEquals(Cosmetic.THEME_NOCTURNE, Shop.equipped(save, Cosmetic.Category.THEME));
    }

    @Test
    @DisplayName("progression rewards cannot be bought, only earned")
    void progressionItemsAreNotForSale() {
        SaveData save = saveWith(10_000);
        assertFalse(Cosmetic.BALL_PASTEL.isForSale());
        assertEquals(Shop.PurchaseResult.NOT_FOR_SALE, Shop.buy(save, Cosmetic.BALL_PASTEL));
        assertEquals(10_000, save.coins);
        assertFalse(Shop.equip(save, Cosmetic.BALL_PASTEL));

        save.recordDailyWin("2026-11-01");
        save.recordDailyWin("2026-11-02");
        save.recordDailyWin("2026-11-03");
        assertTrue(Shop.isOwned(save, Cosmetic.BALL_PASTEL));
        assertTrue(Shop.equip(save, Cosmetic.BALL_PASTEL));
        assertEquals(Cosmetic.BALL_PASTEL, Shop.equipped(save, Cosmetic.Category.BALL_PALETTE));
    }

    @Test
    @DisplayName("equipping needs ownership; the free item is always available")
    void equipRequiresOwnership() {
        SaveData save = saveWith(0);
        assertFalse(Shop.equip(save, Cosmetic.TUBE_GOLD));
        assertEquals(Cosmetic.TUBE_GLASS, Shop.equipped(save, Cosmetic.Category.TUBE));
        assertTrue(Shop.equip(save, Cosmetic.TUBE_GLASS));
    }

    @Test
    @DisplayName("an equipped id that is unknown or no longer owned falls back to the free item")
    void equippedFallsBack() {
        SaveData save = saveWith(500);
        assertEquals(Shop.PurchaseResult.BOUGHT, Shop.buy(save, Cosmetic.FINISH_MATTE));
        save.reset(); // purchases go with the progress
        assertEquals(Cosmetic.FINISH_GLOSSY, Shop.equipped(save, Cosmetic.Category.BALL_FINISH));

        save.setEquippedCosmetic(Cosmetic.Category.THEME, "theme.removed");
        assertEquals(Cosmetic.THEME_NOCTURNE, Shop.equipped(save, Cosmetic.Category.THEME));
    }

    @Test
    @DisplayName("newly affordable items are announced once each")
    void newlyAffordableOnce() {
        SaveData save = saveWith(260);
        List<Cosmetic> fresh = Shop.newlyAffordable(save);
        assertEquals(countForSale(250), fresh.size()); // every 250-coin item, no 500s
        assertTrue(fresh.size() >= 3);
        for (Cosmetic item : fresh) {
            assertEquals(250, item.price());
            save.markShopNotified(item.id());
        }
        assertTrue(Shop.newlyAffordable(save).isEmpty());
        save.addCoins(300); // 560: every tier above 250 up to 500 comes within reach
        List<Cosmetic> next = Shop.newlyAffordable(save);
        int between = countForSale(300) + countForSale(400) + countForSale(500);
        assertEquals(between, next.size());
        for (Cosmetic item : next) {
            assertTrue(item.price() > 250 && item.price() <= 560, item.id());
        }
        assertEquals(countForSale(250) + between, Shop.affordableCount(save));
    }

    @Test
    @DisplayName("the catalogue is deep enough that a regular player keeps finding things to buy")
    void catalogueDepth() {
        int total = 0;
        int items = 0;
        for (Cosmetic item : Cosmetic.ALL) {
            if (item.isForSale()) {
                total += item.price();
                items++;
            }
        }
        assertTrue(items >= 20, "for-sale items: " + items);
        // At roughly 40 coins a level, the whole catalogue is about 300 levels of play.
        assertTrue(total >= 12_000, "catalogue value: " + total);
        for (Cosmetic.Category category : Cosmetic.Category.values()) {
            int forSale = 0;
            for (Cosmetic item : Cosmetic.of(category)) {
                if (item.isForSale()) forSale++;
            }
            assertTrue(forSale >= 4, category + " needs at least four items to buy");
        }
    }

    @Test
    @DisplayName("filters split a tab into owned, buyable and earnable, affordable first")
    void filtersPartitionATab() {
        SaveData save = saveWith(400);
        List<Cosmetic> balls = Cosmetic.of(Cosmetic.Category.BALL_PALETTE);

        List<Cosmetic> owned = Shop.filter(save, balls, Shop.Filter.OWNED);
        List<Cosmetic> coins = Shop.filter(save, balls, Shop.Filter.COINS);
        List<Cosmetic> earn = Shop.filter(save, balls, Shop.Filter.EARN);

        assertEquals(balls.size(), Shop.filter(save, balls, Shop.Filter.ALL).size());
        assertEquals(balls.size(), owned.size() + coins.size() + earn.size(), "no item lost or doubled");
        assertTrue(owned.contains(Cosmetic.BALL_CLASSIC));
        for (Cosmetic item : earn) {
            assertTrue(item.unlock() != Cosmetic.Unlock.NONE, item.id());
        }
        // Nearest goal first: the 30-star palette (fresh save) heads the list.
        assertEquals(Cosmetic.BALL_NEON, earn.get(0));
        // Affordable items lead, cheapest first, then the rest by price.
        assertTrue(coins.get(0).price() <= 400);
        boolean seenUnaffordable = false;
        for (Cosmetic item : coins) {
            if (item.price() > 400) seenUnaffordable = true;
            else assertTrue(!seenUnaffordable, "affordable " + item.id() + " listed after an unaffordable one");
        }
    }

    private static int countForSale(int price) {
        int count = 0;
        for (Cosmetic item : Cosmetic.ALL) {
            if (item.isForSale() && item.price() == price) count++;
        }
        return count;
    }
}
