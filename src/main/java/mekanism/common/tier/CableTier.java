package mekanism.common.tier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.config.MekanismConfig;

public enum CableTier implements ITier<CableTier> {
    BASIC(3200),
    ADVANCED(12800),
    ELITE(64000),
    ULTIMATE(320000);

    private final int baseCapacity;
    private final BaseTier baseTier;

    CableTier(int capacity) {
        baseCapacity = capacity;
        baseTier = BaseTier.get(ordinal());
    }

    public static CableTier getDefault() {
        return BASIC;
    }

    public static CableTier get(int index) {
        if (index < 0 || index >= values().length) {
            return getDefault();
        }
        return values()[index];
    }

    public static CableTier get(@Nonnull BaseTier tier) {
        return get(tier.ordinal());
    }

    @Override
    public boolean hasNext() {
        return ordinal() + 1 < values().length;
    }

    @Nullable
    @Override
    public CableTier next() {
        return hasNext() ? get(ordinal() + 1) : null;
    }

    @Override
    public BaseTier getBaseTier() {
        return baseTier;
    }

    public int getCableCapacity() {
        return MekanismConfig.current().general.tiers.get(baseTier).CableCapacity.val();
    }

    public int getBaseCapacity() {
        return baseCapacity;
    }
}