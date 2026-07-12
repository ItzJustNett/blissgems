package dev.xoperr.blissgems.utils;

public enum Achievement {
    SHATTERED("Shattered", "Reach the Broken energy stage", 1),
    ZIP_AWAY("Zip Away", "Travel across 1,914 copper blocks using Conduction", 1914),
    PIERCING_PRECISION("Piercing Precision", "Hit a player with 5 consecutive Phantom Daggers", 5),
    SAVED_BY_THE_DICE("Saved By The Dice", "Avoid a fatal blow with Phasing", 1),
    OVERFLOWING("Overflowing", "Reach the Pristine +5 energy stage", 1),
    ALMOST_FELL_FROM_GRACE("Almost Fell From Grace", "Prevent 30,000 points of fall damage with Puff Gem", 30000),
    GOOD_AS_NEW("Good As New!", "Draw 10 energy from the Energy Pool using a Repair Kit", 10),
    REAWAKENING("Reawakening", "Successfully complete a Restoration Ritual", 1),
    ITS_RABBIT_SEASON("It's Rabbit Season", "Track the same player with Shadow Stalker 17 times", 17),
    TIME_FOR_A_CHANGE("Time For A Change", "Use a Trader to switch gem types", 1),
    THE_NEXT_LEVEL("The Next Level", "Use an Upgrader to obtain a Tier 2 gem", 1),
    OVERCHARGED("Overcharged", "Charge a Flux Gem to 100W", 1),
    BOUNDARY_BREAK("Boundary Break", "Boost enchantments beyond vanilla levels using Amplification", 1);

    private final String displayName;
    private final String description;
    private final int targetProgress;

    Achievement(String displayName, String description, int targetProgress) {
        this.displayName = displayName;
        this.description = description;
        this.targetProgress = targetProgress;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getTargetProgress() {
        return targetProgress;
    }
}
