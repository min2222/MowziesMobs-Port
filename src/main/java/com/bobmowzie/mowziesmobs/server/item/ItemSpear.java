package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.server.capability.CapabilityHandler;
import com.bobmowzie.mowziesmobs.server.capability.PlayerCapability;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemTier;
import net.minecraft.world.item.ToolItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.RayTraceContext;
import net.minecraft.util.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemSpear extends MowzieToolItem {
    public ItemSpear(Item.Properties properties) {
        super(-2 + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.BARAKOA_SPEAR.toolConfig.attackDamage.get().floatValue(), -4f + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.BARAKOA_SPEAR.toolConfig.attackSpeed.get().floatValue(), ItemTier.STONE, Sets.newHashSet(), properties);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.type == EnchantmentType.WEAPON || enchantment.type == EnchantmentType.BREAKABLE;
    }

    public static LivingEntity raytraceEntities(Level world, Player player, double range) {
        ItemSpear.HitResult result = new ItemSpear.HitResult();
        Vec3 pos = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vec3 segment = player.getLookVec();
        segment = pos.add(segment.x * range, segment.y * range, segment.z * range);
        result.setBlockHit(world.rayTraceBlocks(new RayTraceContext(pos, segment, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player)));
        double collidePosX, collidePosY, collidePosZ;
        if (result.blockHit != null) {
            collidePosX = result.blockHit.getHitVec().x;
            collidePosY = result.blockHit.getHitVec().y;
            collidePosZ = result.blockHit.getHitVec().z;
        }
        else {
            Vec3 end = player.getLookVec().scale(range).add(pos);
            collidePosX = end.x;
            collidePosY = end.y;
            collidePosZ = end.z;
        }

        List<LivingEntity> entities = world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(Math.min(pos.x, collidePosX), Math.min(pos.y, collidePosY), Math.min(pos.z, collidePosZ), Math.max(pos.x, collidePosX), Math.max(pos.y, collidePosY), Math.max(pos.z, collidePosZ)).grow(1, 1, 1));
        LivingEntity closest = null;
        for (LivingEntity entity : entities) {
            if (entity == player) {
                continue;
            }
            float pad = entity.getCollisionBorderSize();
            AxisAlignedBB aabb = entity.getBoundingBox().grow(pad, pad, pad);
            boolean hit = aabb.intersects(pos, segment);
            if (aabb.contains(pos) || hit) {
                result.addEntityHit(entity);
                if (closest == null || player.getDistance(closest) > player.getDistance(entity)) closest = entity;
            }
        }
        return closest;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<TextComponent> tooltip, ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TextComponent(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
        tooltip.add(new TextComponent(getDescriptionId() + ".text.1").setStyle(ItemHandler.TOOLTIP_STYLE));
    }

    @Override
    public ConfigHandler.ToolConfig getConfig() {
        return ConfigHandler.COMMON.TOOLS_AND_ABILITIES.BARAKOA_SPEAR.toolConfig;
    }

    public static class HitResult {
        private HitResult blockHit;

        private final List<LivingEntity> entities = new ArrayList<>();

        public HitResult getBlockHit() {
            return blockHit;
        }

        public void setBlockHit(HitResult blockHit) {
            this.blockHit = blockHit;
        }

        public void addEntityHit(LivingEntity entity) {
            entities.add(entity);
        }
    }
}
