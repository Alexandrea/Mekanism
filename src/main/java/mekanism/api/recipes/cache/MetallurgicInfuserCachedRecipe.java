package mekanism.api.recipes.cache;

import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.annotations.NonNull;
import mekanism.api.infuse.InfuseObject;
import mekanism.api.recipes.MetallurgicInfuserRecipe;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.common.util.FieldsAreNonnullByDefault;
import net.minecraft.item.ItemStack;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public class MetallurgicInfuserCachedRecipe extends CachedRecipe<MetallurgicInfuserRecipe> {

    private final IOutputHandler<@NonNull ItemStack> outputHandler;
    private final IInputHandler<@NonNull InfuseObject> infusionInputHandler;
    private final IInputHandler<@NonNull ItemStack> itemInputHandler;

    public MetallurgicInfuserCachedRecipe(MetallurgicInfuserRecipe recipe, IInputHandler<@NonNull InfuseObject> infusionInputHandler,
          IInputHandler<@NonNull ItemStack> itemInputHandler, IOutputHandler<@NonNull ItemStack> outputHandler) {
        super(recipe);
        this.infusionInputHandler = infusionInputHandler;
        this.itemInputHandler = itemInputHandler;
        this.outputHandler = outputHandler;
    }

    @Override
    protected int getOperationsThisTick(int currentMax) {
        currentMax = super.getOperationsThisTick(currentMax);
        if (currentMax == 0) {
            //If our parent checks show we can't operate then return so
            return 0;
        }
        //TODO: This input getting, is only really needed for getting the output
        ItemStack recipeItem = itemInputHandler.getRecipeInput(recipe.getItemInput());
        //Test to make sure we can even perform a single operation. This is akin to !recipe.test(inputItem)
        if (recipeItem.isEmpty()) {
            return 0;
        }

        //Now check the infusion input
        InfuseObject recipeInfuseObject = infusionInputHandler.getRecipeInput(recipe.getInfusionInput());
        if (recipeInfuseObject == null || recipeInfuseObject.isEmpty()) {
            //TODO: 1.14 have there be an "EMPTY" object instance so that it can never be null
            return 0;
        }

        //Calculate the current max based on the item input
        currentMax = itemInputHandler.operationsCanSupport(recipe.getItemInput(), currentMax);

        //Calculate the current max based on the infusion input
        currentMax = infusionInputHandler.operationsCanSupport(recipe.getInfusionInput(), currentMax);

        //Calculate the max based on the space in the output
        return outputHandler.operationsRoomFor(recipe.getOutput(recipeInfuseObject, recipeItem), currentMax);
    }

    @Override
    public boolean isInputValid() {
        return recipe.test(infusionInputHandler.getInput(), itemInputHandler.getInput());
    }

    @Override
    protected void finishProcessing(int operations) {
        //TODO: Cache this stuff from when getOperationsThisTick was called?
        ItemStack recipeItem = itemInputHandler.getRecipeInput(recipe.getItemInput());
        if (recipeItem.isEmpty()) {
            //Something went wrong, this if should never really be true if we got to finishProcessing
            return;
        }

        InfuseObject recipeInfuseObject = infusionInputHandler.getRecipeInput(recipe.getInfusionInput());
        if (recipeInfuseObject == null || recipeInfuseObject.isEmpty()) {
            //TODO: 1.14 have there be an "EMPTY" object instance so that it can never be null
            //Something went wrong, this if should never really be true if we got to finishProcessing
            return;
        }
        infusionInputHandler.use(recipeInfuseObject, operations);
        itemInputHandler.use(recipeItem, operations);
        outputHandler.handleOutput(recipe.getOutput(recipeInfuseObject, recipeItem), operations);
    }
}