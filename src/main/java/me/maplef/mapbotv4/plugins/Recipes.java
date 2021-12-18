package me.maplef.mapbotv4.plugins;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public class Recipes {
    public static final ShapedRecipe elytra = new ShapedRecipe(NamespacedKey.minecraft("newelytra"), new ItemStack(Material.ELYTRA))
            .shape("aba", "cdc","cec")
            .setIngredient('a', Material.NETHER_STAR)
            .setIngredient('b', Material.END_CRYSTAL)
            .setIngredient('c', Material.NETHERITE_INGOT)
            .setIngredient('d', Material.AMETHYST_CLUSTER)
            .setIngredient('e', Material.SADDLE);
}
