package mekanism.api.recipes.cache;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.annotations.NonNull;
import mekanism.api.gas.GasStack;
import mekanism.api.recipes.ChemicalInfuserRecipe;
import mekanism.api.recipes.inputs.GasStackIngredient;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.common.util.FieldsAreNonnullByDefault;
import org.apache.commons.lang3.tuple.Pair;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public class ChemicalInfuserCachedRecipe extends CachedRecipe<ChemicalInfuserRecipe> {

    private final IOutputHandler<@NonNull GasStack> outputHandler;
    private final IInputHandler<@NonNull GasStack> leftInputHandler;
    private final IInputHandler<@NonNull GasStack> rightInputHandler;

    public ChemicalInfuserCachedRecipe(ChemicalInfuserRecipe recipe, IInputHandler<@NonNull GasStack> leftInputHandler, IInputHandler<@NonNull GasStack> rightInputHandler,
          IOutputHandler<@NonNull GasStack> outputHandler) {
        super(recipe);
        this.leftInputHandler = leftInputHandler;
        this.rightInputHandler = rightInputHandler;
        this.outputHandler = outputHandler;
    }

    @Nullable
    private Pair<GasStackIngredient, GasStackIngredient> getIngredients() {
        GasStack leftInputGas = leftInputHandler.getInput();
        if (leftInputGas == null || leftInputGas.amount == 0) {
            return null;
        }
        GasStack rightInputGas = rightInputHandler.getInput();
        if (rightInputGas == null || rightInputGas.amount == 0) {
            return null;
        }

        //TODO: Convert this stuff to using the leftInputHandler and rightInputHandler
        GasStackIngredient leftInput = recipe.getLeftInput();
        GasStackIngredient rightInput = recipe.getRightInput();
        if (!leftInput.test(leftInputGas) || !rightInput.test(rightInputGas)) {
            //If one of our inputs is invalid for the side it is on, switch them so that we can check
            // if they are just reversed which side they are on and there is a valid recipe for them
            // if they are on the other side
            return Pair.of(rightInput, leftInput);
        }
        return Pair.of(leftInput, rightInput);
    }

    @Override
    protected int getOperationsThisTick(int currentMax) {
        currentMax = super.getOperationsThisTick(currentMax);
        if (currentMax == 0) {
            //If our parent checks show we can't operate then return so
            return 0;
        }
        Pair<GasStackIngredient, GasStackIngredient> ingredients = getIngredients();
        if (ingredients == null) {
            //If either inputs are empty then we are unable to operate
            return 0;
        }
        GasStack leftRecipeInput = leftInputHandler.getRecipeInput(ingredients.getLeft());
        //Test to make sure we can even perform a single operation. This is akin to !recipe.test(leftInputGas)
        if (leftRecipeInput == null || leftRecipeInput.amount == 0) {
            return 0;
        }
        GasStack rightRecipeInput = rightInputHandler.getRecipeInput(ingredients.getRight());
        //Test to make sure we can even perform a single operation. This is akin to !recipe.test(rightInputGas)
        if (rightRecipeInput == null || rightRecipeInput.amount == 0) {
            return 0;
        }
        //Calculate the current max based on the left input
        currentMax = leftInputHandler.operationsCanSupport(ingredients.getLeft(), currentMax);
        //Calculate the current max based on the right input
        currentMax = rightInputHandler.operationsCanSupport(ingredients.getRight(), currentMax);
        //Calculate the max based on the space in the output
        return outputHandler.operationsRoomFor(recipe.getOutput(leftRecipeInput, rightRecipeInput), currentMax);
    }

    @Override
    public boolean isInputValid() {
        GasStack leftInput = leftInputHandler.getInput();
        GasStack rightInput = rightInputHandler.getInput();
        return leftInput != null && rightInput != null && recipe.test(leftInput, rightInput);
    }

    @Override
    protected void finishProcessing(int operations) {
        //TODO: Cache this stuff from when getOperationsThisTick was called?
        Pair<GasStackIngredient, GasStackIngredient> ingredients = getIngredients();
        if (ingredients == null) {
            //Something went wrong, this if should never really be true if we got to finishProcessing
            return;
        }
        GasStack leftRecipeInput = leftInputHandler.getRecipeInput(ingredients.getLeft());
        //Test to make sure we can even perform a single operation. This is akin to !recipe.test(leftInputGas)
        if (leftRecipeInput == null || leftRecipeInput.amount == 0) {
            //Something went wrong, this if should never really be true if we got to finishProcessing
            return;
        }
        GasStack rightRecipeInput = rightInputHandler.getRecipeInput(ingredients.getRight());
        //Test to make sure we can even perform a single operation. This is akin to !recipe.test(rightInputGas)
        if (rightRecipeInput == null || rightRecipeInput.amount == 0) {
            //Something went wrong, this if should never really be true if we got to finishProcessing
            return;
        }
        leftInputHandler.use(leftRecipeInput, operations);
        rightInputHandler.use(rightRecipeInput, operations);
        outputHandler.handleOutput(recipe.getOutput(leftRecipeInput, rightRecipeInput), operations);
    }
}