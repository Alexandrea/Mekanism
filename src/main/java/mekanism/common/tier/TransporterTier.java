package mekanism.common.tier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.config.MekanismConfig;

public enum TransporterTier implements ITier<TransporterTier> {
    BASIC(1, 5),
    ADVANCED(16, 10),
    ELITE(32, 20),
    ULTIMATE(64, 50);

    private final int basePull;
    private final int baseSpeed;
    private final BaseTier baseTier;

    TransporterTier(int pull, int s) {
        basePull = pull;
        baseSpeed = s;
        baseTier = BaseTier.get(ordinal());
    }

    public static TransporterTier getDefault() {
        return BASIC;
    }

    public static TransporterTier get(int index) {
        if (index < 0 || index >= values().length) {
            return getDefault();
        }
        return values()[index];
    }

    public static TransporterTier get(@Nonnull BaseTier tier) {
        return get(tier.ordinal());
    }

    @Override
    public boolean hasNext() {
        return ordinal() + 1 < values().length;
    }

    @Nullable
    @Override
    public TransporterTier next() {
        return hasNext() ? get(ordinal() + 1) : null;
    }

    @Override
    public BaseTier getBaseTier() {
        return baseTier;
    }

    public int getPullAmount() {
        return MekanismConfig.current().general.tiers.get(baseTier).TransporterPullAmount.val();
    }

    public int getSpeed() {
        return MekanismConfig.current().general.tiers.get(baseTier).TransporterSpeed.val();
    }

    public int getBasePull() {
        return basePull;
    }

    public int getBaseSpeed() {
        return baseSpeed;
    }
}