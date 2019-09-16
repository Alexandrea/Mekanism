package mekanism.api.recipes;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import mekanism.api.annotations.NonNull;
import mekanism.api.gas.GasStack;
import mekanism.api.recipes.inputs.GasStackIngredient;
import mekanism.api.recipes.inputs.ItemStackIngredient;
import mekanism.api.recipes.outputs.OreDictSupplier;
import mekanism.common.util.FieldsAreNonnullByDefault;
import net.minecraft.item.ItemStack;

/**
 * Inputs: ItemStack + GasStack Output: ItemStack
 *
 * Ex-AdvancedMachineInput based; InjectionRecipe, OsmiumCompressorRecipe, PurificationRecipe
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
//TODO: Make a note of the fact this recipe uses the size of the gas input as a base, but still for the most part will end up multiplying it
// by a per tick usage
public class ItemStackGasToItemStackRecipe implements IMekanismRecipe, BiPredicate<@NonNull ItemStack, @NonNull GasStack> {

    private final ItemStackIngredient itemInput;
    private final GasStackIngredient gasInput;
    private final ItemStack outputDefinition;

    public ItemStackGasToItemStackRecipe(ItemStackIngredient itemInput, GasStackIngredient gasInput, ItemStack outputDefinition) {
        this.itemInput = itemInput;
        this.gasInput = gasInput;
        this.outputDefinition = outputDefinition;
    }

    public ItemStackIngredient getItemInput() {
        return itemInput;
    }

    public GasStackIngredient getGasInput() {
        return gasInput;
    }

    public ItemStack getOutput(ItemStack inputItem, GasStack inputGas) {
        return outputDefinition.copy();
    }

    @Override
    public boolean test(@NonNull ItemStack itemStack, @NonNull GasStack gasStack) {
        return itemInput.test(itemStack) && gasInput.test(gasStack);
    }

    public @NonNull List<@NonNull ItemStack> getOutputDefinition() {
        return Collections.singletonList(outputDefinition);
    }

    public static class ItemStackGasToItemStackRecipeOre extends ItemStackGasToItemStackRecipe {

        private final OreDictSupplier outputSupplier;

        public ItemStackGasToItemStackRecipeOre(ItemStackIngredient itemInput, GasStackIngredient gasInput, String outputOreName) {
            super(itemInput, gasInput, ItemStack.EMPTY);
            this.outputSupplier = new OreDictSupplier(outputOreName);
        }

        @Override
        public ItemStack getOutput(ItemStack inputItem, GasStack inputGas) {
            return this.outputSupplier.get();
        }

        @Override
        public @NonNull List<@NonNull ItemStack> getOutputDefinition() {
            return this.outputSupplier.getPossibleOutputs();
        }
    }
}
