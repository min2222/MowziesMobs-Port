package com.bobmowzie.mowziesmobs.common.item;

import com.bobmowzie.mowziesmobs.common.creativetab.MMTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemWroughtHelm extends ItemArmor {
    public ItemWroughtHelm()
    {
        super(ArmorMaterial.IRON, 2, 0);
        setUnlocalizedName("wroughtHelm");
        setCreativeTab(MMTabs.generic);
    }

    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
        super.onArmorTick(world, player, itemStack);
    }

    @Override
    public boolean getIsRepairable(ItemStack p_82789_1_, ItemStack p_82789_2_) {
        return false;
    }

    @Override
    public boolean isDamageable() {
        return false;
    }
}
