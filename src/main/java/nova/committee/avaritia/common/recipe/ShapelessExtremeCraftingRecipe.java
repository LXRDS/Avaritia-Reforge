package nova.committee.avaritia.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.ForgeRegistryEntry;
import nova.committee.avaritia.init.registry.ModRecipeTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Description:
 * Author: cnlimiter
 * Date: 2022/4/2 9:16
 * Version: 1.0
 */
public class ShapelessExtremeCraftingRecipe implements ISpecialRecipe, ICraftRecipe {
    private final ResourceLocation recipeId;
    private final NonNullList<Ingredient> inputs;
    private final ItemStack output;
    private Map<Integer, Function<ItemStack, ItemStack>> transformers;


    public ShapelessExtremeCraftingRecipe(ResourceLocation recipeId, NonNullList<Ingredient> inputs, ItemStack output) {
        this.recipeId = recipeId;
        this.inputs = inputs;
        this.output = output;

    }

    private static int getTierFromSize(int size) {
        return size < 10 ? 1
                : size < 26 ? 2
                : size < 50 ? 3
                : 4;
    }

    @Override
    public ItemStack getResultItem() {
        return this.output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.inputs;
    }

    @Override
    public ResourceLocation getId() {
        return this.recipeId;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SHAPELESS_EXTREME_CRAFT_RECIPE;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.RecipeTypes.CRAFTING;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.inputs.size();
    }

    @Override
    public ItemStack assemble(IItemHandler inventory) {
        return this.output.copy();
    }

    @Override
    public ItemStack assemble(Container inv) {
        return this.output.copy();
    }

    @Override
    public boolean matches(IItemHandler inventory) {

        List<ItemStack> inputs = new ArrayList<>();
        int matched = 0;

        for (int i = 0; i < inventory.getSlots(); i++) {
            var stack = inventory.getStackInSlot(i);

            if (!stack.isEmpty()) {
                inputs.add(stack);

                matched++;
            }
        }

        return matched == this.inputs.size() && RecipeMatcher.findMatches(inputs, this.inputs) != null;
    }

    @Override
    public boolean matches(Container inv, Level level) {
        return this.matches(new InvWrapper(inv));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(Container inv) {
        if (this.transformers != null) {
            NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

            this.transformers.forEach((i, stack) -> {
                remaining.set(i, stack.apply(inv.getItem(i)));
            });

            return remaining;
        }

        return ISpecialRecipe.super.getRemainingItems(inv);
    }

    public void setTransformers(Map<Integer, Function<ItemStack, ItemStack>> transformers) {
        this.transformers = transformers;
    }

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ShapelessExtremeCraftingRecipe> {
        @Override
        public ShapelessExtremeCraftingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            NonNullList<Ingredient> inputs = NonNullList.create();
            var ingredients = GsonHelper.getAsJsonArray(json, "ingredients");

            for (int i = 0; i < ingredients.size(); i++) {
                inputs.add(Ingredient.fromJson(ingredients.get(i)));
            }

            var output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

            return new ShapelessExtremeCraftingRecipe(recipeId, inputs, output);
        }

        @Override
        public ShapelessExtremeCraftingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            var inputs = NonNullList.withSize(size, Ingredient.EMPTY);

            for (int i = 0; i < size; ++i) {
                inputs.set(i, Ingredient.fromNetwork(buffer));
            }

            var output = buffer.readItem();

            return new ShapelessExtremeCraftingRecipe(recipeId, inputs, output);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShapelessExtremeCraftingRecipe recipe) {
            buffer.writeVarInt(recipe.inputs.size());

            for (var ingredient : recipe.inputs) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItem(recipe.output);
        }
    }

}
