package mekanism.common.tile;

import mekanism.api.providers.IBlockProvider;
import mekanism.common.block.basic.BlockInductionProvider;
import mekanism.common.tier.InductionProviderTier;
import mekanism.common.tile.base.TileEntityMekanism;

public class TileEntityInductionProvider extends TileEntityMekanism {

    public InductionProviderTier tier;

    public TileEntityInductionProvider(IBlockProvider blockProvider) {
        super(blockProvider);
    }

    @Override
    protected void presetVariables() {
        tier = ((BlockInductionProvider) getBlockType()).getTier();
    }

    @Override
    public void onUpdate() {
    }
}