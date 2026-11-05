package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.Achievement;
import com.ben.laama.sortpuzzle.logic.ChallengeRules;
import com.ben.laama.sortpuzzle.logic.Cosmetic;
import com.ben.laama.sortpuzzle.logic.Shop;
import com.ben.laama.sortpuzzle.logic.StarRoad;
import com.ben.laama.sortpuzzle.logic.WeeklyGoals;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.services.AnalyticsEvents;
import com.ben.laama.sortpuzzle.services.GameServices;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

import java.time.LocalDate;

/** Weekly goals, achievements, collectible colour themes and custom challenge selection. */
public final class ProgressScreen extends BaseScreen {

    private final SaveData save;
    private final Ui.Toggle noUndo;
    private final Ui.Toggle noHints;
    private final Ui.Toggle noBoosters;
    private final Ui.Toggle parLimit;
    private final TextButton startChallenge;

    public ProgressScreen(Main game) {
        super(game);
        save = game.save();
        save.beginWeeklyGoals(LocalDate.now(java.time.ZoneOffset.UTC));

        Table root = new Table();
        root.setFillParent(true);
        safeArea.addActor(root);

        Table header = new Table();
        header.pad(20f, 26f, 8f, 26f);
        header.add(Ui.iconButton(skin, "icon-back", game::goHome)).size(Ui.ICON_BUTTON).left();
        header.add(new Label("Goals & challenges", skin, "title")).expandX();
        header.add().size(Ui.ICON_BUTTON);
        root.add(header).growX().row();

        Table content = new Table();
        content.pad(12f, 24f, 32f, 24f).defaults().width(580f).padBottom(18f);
        content.add(weeklyCard()).row();
        content.add(starRoadCard()).row();
        content.add(achievementCard()).row();
        content.add(cosmeticCard()).row();
        if (game.services().isAvailable()) {
            content.add(leaderboardCard()).row(); // only where Play Games exists and is configured
        }

        Table challenge = Ui.card(skin);
        challenge.add(new Label("BUILD YOUR CHALLENGE", skin, "heading")).left().row();
        challenge.add(Ui.paragraph(skin,
            "Choose any combination. The challenge uses your current campaign level and never "
                + "takes away campaign progress.", "caption", 520f))
            .width(520f).left().padTop(6f).row();
        noUndo = challengeToggle(challenge, "No undo");
        noHints = challengeToggle(challenge, "No hints");
        noBoosters = challengeToggle(challenge, "No extra tubes");
        parLimit = challengeToggle(challenge, "Par move limit");
        startChallenge = Ui.button(skin, "CHOOSE A RULE TO START", "primary",
            this::startChallenge);
        startChallenge.setDisabled(true);
        challenge.add(startChallenge).growX().height(72f).padTop(10f).row();
        content.add(challenge).row();

        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setScrollingDisabled(true, false);
        root.add(scroll).grow().row();
    }

    /**
     * Google Play Games leaderboards. Signed out: one button that signs in and then opens the
     * boards; signed in: the player's name and a button straight to the boards. Optional and
     * explicit: nothing here is required to play.
     */
    private Table leaderboardCard() {
        GameServices services = game.services();
        Table card = Ui.card(skin);
        card.add(new Label("LEADERBOARDS", skin, "heading")).left().row();
        Label status = new Label("", skin, "caption");
        status.setWrap(true);
        card.add(status).width(520f).left().padTop(6f).row();
        TextButton open = Ui.button(skin, "", "primary", () -> { });
        card.add(open).growX().height(72f).padTop(12f).row();
        refreshLeaderboardCard(status, open);
        open.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                openLeaderboards(status, open);
            }
        });
        return card;
    }

    private void refreshLeaderboardCard(Label status, TextButton open) {
        GameServices services = game.services();
        if (services.isSignedIn()) {
            String name = services.playerName();
            status.setText(name.isEmpty() ? "Signed in to Google Play Games."
                : "Signed in as " + name + ".");
            open.setText("VIEW LEADERBOARDS");
        } else {
            status.setText("Compare your level, stars and daily moves with other players. "
                + "Sign-in is optional and uses your Google Play Games profile.");
            open.setText("SIGN IN WITH PLAY GAMES");
        }
    }

    private void openLeaderboards(Label status, TextButton open) {
        GameServices services = game.services();
        if (services.isSignedIn()) {
            game.analytics().event(AnalyticsEvents.LEADERBOARD_OPEN);
            services.showLeaderboards();
            return;
        }
        open.setDisabled(true);
        services.signIn(signedIn -> {
            open.setDisabled(false);
            game.analytics().event(AnalyticsEvents.PLAY_GAMES_SIGN_IN,
                AnalyticsEvents.P_RESULT, signedIn ? "ok" : "failed");
            refreshLeaderboardCard(status, open);
            if (signedIn) {
                game.syncLeaderboards();
                services.showLeaderboards();
            } else {
                Ui.toast(stage, skin, "Couldn't sign in to Play Games. Try again later.",
                    Palette.TEXT_DIM);
            }
        });
    }

    private Table weeklyCard() {
        Table card = Ui.card(skin);
        card.add(new Label("THIS WEEK", skin, "heading")).left().row();
        card.add(new Label("New goals every Monday (UTC)  •  "
            + GameConfig.WEEKLY_GOAL_REWARD + " coins each", skin, "caption"))
            .left().padTop(4f).row();
        for (int goal = 0; goal < WeeklyGoals.GOAL_COUNT; goal++) {
            Table row = new Table();
            row.add(new Label(WeeklyGoals.title(goal), skin, "label")).left().expandX();
            boolean done = (save.weeklyClaimedMask & (1 << goal)) != 0;
            String progress = Math.min(save.weeklyProgress(goal), WeeklyGoals.target(goal))
                + " / " + WeeklyGoals.target(goal);
            row.add(new Label(done ? "DONE" : progress, skin, done ? "gold" : "caption")).right();
            card.add(row).growX().padTop(14f).row();
        }
        return card;
    }

    private Table achievementCard() {
        Table card = Ui.card(skin);
        card.add(new Label("ACHIEVEMENTS", skin, "heading")).left().row();
        card.add(new Label(save.unlockedAchievementCount() + " / "
            + Achievement.values().length + " unlocked", skin, "caption"))
            .left().padTop(4f).row();
        for (Achievement achievement : Achievement.values()) {
            boolean unlocked = save.isAchievementUnlocked(achievement);
            Table row = new Table();
            row.add(new Label(achievement.title(), skin, "label")).left().expandX();
            int current = Math.min(save.achievementProgress(achievement), achievement.target());
            row.add(new Label(unlocked ? "UNLOCKED  •  +" + achievement.reward()
                : current + " / " + achievement.target(), skin,
                unlocked ? "gold" : "caption")).right();
            card.add(row).growX().padTop(12f).row();
            card.add(new Label(achievement.description(), skin, "caption")).left().row();
        }
        return card;
    }

    /**
     * The star road: total stars, a bar to the next milestone and every skin on the road with
     * its state. Stars are never spent, so the list only ever fills up.
     */
    private Table starRoadCard() {
        int stars = save.totalStars();
        Table card = Ui.card(skin);
        Table head = new Table();
        head.add(new Label("STAR ROAD", skin, "heading")).left().expandX();
        Label total = new Label(String.valueOf(stars), skin, "gold");
        head.add(Ui.chip(skin, "icon-star", Palette.GOLD, total)).right();
        card.add(head).growX().row();
        card.add(Ui.paragraph(skin, "Every star you earn (1 to 3 per level, best result kept) "
            + "counts. Replay levels for 3 stars to unlock exclusive skins.", "caption", 520f))
            .width(520f).left().padTop(4f).row();

        StarRoad.Milestone next = StarRoad.next(stars);
        if (next != null) {
            float progress = StarRoad.progressTo(next, stars);
            Table bar = new Table();
            // Plain rectangles: a "pill" nine-patch is taller than this bar and would spill.
            bar.setBackground(skin.newDrawable("white", Palette.SURFACE_STRONG));
            Table fill = new Table();
            fill.setBackground(skin.newDrawable("white", Palette.GOLD));
            bar.add(fill).width(Math.max(4f, 520f * progress)).height(8f).left().expandX();
            card.add(bar).width(520f).height(8f).padTop(14f).row();
            card.add(new Label((next.stars - stars) + " more to " + next.reward.displayName(),
                skin, "caption")).left().padTop(6f).row();
        } else {
            card.add(new Label("Road complete: every star skin is yours", skin, "caption"))
                .left().padTop(10f).row();
        }

        // The road is long (a quarter of the 400 skins): show the last stop reached and the next
        // four, plus the count, rather than a hundred rows.
        int reachedCount = StarRoad.reachedCount(stars);
        card.add(new Label(reachedCount + " / " + StarRoad.MILESTONES.size() + " star skins unlocked",
            skin, "label")).left().padTop(12f).row();
        int from = Math.max(0, reachedCount - 1);
        int to = Math.min(StarRoad.MILESTONES.size(), from + 5);
        for (StarRoad.Milestone milestone : StarRoad.MILESTONES.subList(from, to)) {
            boolean reached = stars >= milestone.stars;
            Table row = new Table();
            Table need = new Table();
            need.add(new Image(skin.newDrawable("icon-star", reached ? Palette.GOLD : Palette.TEXT_DIM)))
                .size(20f).padRight(6f);
            Label count = new Label(String.valueOf(milestone.stars), skin, "label");
            count.setColor(reached ? Palette.GOLD : Palette.TEXT_DIM);
            need.add(count);
            row.add(need).width(90f).left();
            Label name = new Label(milestone.reward.displayName() + "  •  "
                + milestone.reward.category().title(), skin, "label");
            name.setColor(reached ? Palette.TEXT : Palette.TEXT_DIM);
            row.add(name).left().expandX();
            row.add(new Image(skin.newDrawable(reached ? "icon-check" : "icon-lock",
                reached ? Palette.SUCCESS : Palette.TEXT_DIM))).size(22f).right();
            card.add(row).growX().padTop(12f).row();
        }
        return card;
    }

    private Table cosmeticCard() {
        Table card = Ui.card(skin);
        card.add(new Label("STYLES", skin, "heading")).left().row();
        card.add(Ui.paragraph(skin, "Ball colours, finishes, tubes and themes: earn some by "
            + "playing, buy the rest with coins in the shop.", "caption", 520f))
            .width(520f).left().padTop(4f).row();
        Table owned = new Table();
        owned.add(new Label(Shop.ownedCount(save) + " / " + Cosmetic.ALL.size() + " owned",
            skin, "label")).left().expandX();
        owned.add(Ui.button(skin, "OPEN SHOP", "gold", () -> {
            game.sound().play(SoundManager.Cue.BUTTON);
            game.goShop();
        })).width(150f).right();
        card.add(owned).growX().padTop(10f).row();
        card.add(Ui.divider(skin)).height(2f).growX().padTop(14f).padBottom(12f).row();
        Table labels = new Table();
        labels.add(new Label("Number labels on balls", skin, "label")).left().expandX();
        labels.add(new Ui.Toggle(skin, save.colorLabels, save::setColorLabels)).right();
        card.add(labels).growX();
        return card;
    }

    private Ui.Toggle challengeToggle(Table card, String title) {
        Table row = new Table();
        row.add(new Label(title, skin, "label")).left().expandX();
        Ui.Toggle toggle = new Ui.Toggle(skin, false, value -> refreshChallengeButton());
        row.add(toggle).right();
        card.add(row).growX().padTop(12f).row();
        return toggle;
    }

    private void refreshChallengeButton() {
        boolean selected = noUndo.isOn() || noHints.isOn() || noBoosters.isOn() || parLimit.isOn();
        startChallenge.setDisabled(!selected);
        startChallenge.setText(selected ? "START  •  " + game.save().currentLevel : "CHOOSE A RULE TO START");
    }

    private void startChallenge() {
        ChallengeRules rules = new ChallengeRules(noUndo.isOn(), noHints.isOn(),
            noBoosters.isOn(), parLimit.isOn());
        game.sound().play(SoundManager.Cue.BUTTON);
        game.goToChallenge(rules);
    }

}
