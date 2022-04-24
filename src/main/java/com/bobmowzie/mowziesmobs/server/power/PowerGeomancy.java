package com.bobmowzie.mowziesmobs.server.power;

import com.bobmowzie.mowziesmobs.MowziesMobs;
import com.bobmowzie.mowziesmobs.client.particle.ParticleHandler;
import com.bobmowzie.mowziesmobs.client.particle.ParticleOrb;
import com.bobmowzie.mowziesmobs.client.particle.util.AdvancedParticleBase;
import com.bobmowzie.mowziesmobs.client.particle.util.ParticleComponent;
import com.bobmowzie.mowziesmobs.server.capability.PlayerCapability;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntityBlockSwapper;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntityBoulder;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntityFallingBlock;
import com.bobmowzie.mowziesmobs.server.message.MessagePlayerStartSummonBoulder;
import com.bobmowzie.mowziesmobs.server.potion.EffectHandler;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Direction;
import net.minecraft.sounds.Hand;
import net.minecraft.util.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class PowerGeomancy extends Power {
    public static final double SPAWN_BOULDER_REACH = 5;

    private int doubleTapTimer = 0;

    protected Random rand;

    private int spawnBoulderCooldown = 8;
    private boolean spawningBoulder = false;
    private boolean liftedMouse = true;
    public int spawnBoulderCharge = 0;
    public BlockPos spawnBoulderPos = new BlockPos(0, 0, 0);
    public Vec3 lookPos = new Vec3(0, 0, 0);
    private BlockState spawnBoulderBlock = Blocks.DIRT.defaultBlockState();

    public boolean tunneling;
    public boolean prevUnderground;
    public BlockState justDug = Blocks.DIRT.defaultBlockState();

    public PowerGeomancy(PlayerCapability.PlayerCapabilityImp capability) {
        super(capability);
        rand = new Random();
    }

    @Override
    public void tick(TickEvent.PlayerTickEvent event) {
        super.tick(event);
        Player player = event.player;
        spawnBoulderCooldown -= 1;
        if (doubleTapTimer > 0) doubleTapTimer--;

        //Tunneling
        if (tunneling) {
            player.fallDistance = 0;
            player.abilities.isFlying = false;
            boolean underground = !player.world.getEntitiesWithinAABB(EntityBlockSwapper.class, player.getBoundingBox()).isEmpty();
            if (player.isOnGround() && !underground) tunneling = false;
            Vec3 lookVec = player.getLookVec();
            float tunnelSpeed = 0.3f;
            if (underground) {
                if (player.isSneaking()) {
                    player.setDeltaMovement(lookVec.normalize().scale(tunnelSpeed));
                }
                else {
                    player.setDeltaMovement(lookVec.mul(0.3, 0, 0.3).add(0, 1, 0).normalize().scale(tunnelSpeed));
                }

                List<LivingEntity> entitiesHit = getEntityLivingBaseNearby(player,2, 2, 2, 2);
                for (LivingEntity entityHit : entitiesHit) {
                    entityHit.hurt(DamageSource.causePlayerDamage(player), 6 * ConfigHandler.COMMON.TOOLS_AND_ABILITIES.geomancyAttackMultiplier.get().floatValue());
                }
            }
            else player.setDeltaMovement(player.getDeltaMovement().subtract(0, 0.07, 0));


            if ((player.isSneaking() && lookVec.y < 0) || underground) {
                if (player.tickCount % 16 == 0) player.playSound(MMSounds.EFFECT_GEOMANCY_RUMBLE.get(random.nextInt(3)).get(), 0.6f, 0.5f + random.nextFloat() * 0.2f);
                for (double x = -1; x <= 1; x++) {
                    for (double y = -1; y <= 2; y++) {
                        for (double z = -1; z <= 1; z++) {
                            if (Math.sqrt(x * x + y * y + z * z) > 1.75) continue;
                            BlockPos pos = new BlockPos(player.getX() + x + player.getDeltaMovement().x(), player.getY() + y + player.getDeltaMovement().y() + player.getHeight()/2f, player.getZ() + z + player.getDeltaMovement().z());
                            BlockState blockState = player.level.getBlockState(pos);
                            if (isBlockDiggable(blockState) && blockState.getBlock() != Blocks.BEDROCK) {
                                justDug = blockState;
                                EntityBlockSwapper.swapBlock(player.world, pos, Blocks.AIR.defaultBlockState(), 20, false, false);
                            }
                        }
                    }
                }
            }
            if (!prevUnderground && underground) {
                player.playSound(MMSounds.EFFECT_GEOMANCY_BREAK_MEDIUM.get(random.nextInt(3)).get(), 1f, 0.9f + random.nextFloat() * 0.1f);
                if (player.level.isClientSide)
                    AdvancedParticleBase.spawnParticle(player.world, ParticleHandler.RING2.get(), (float) player.getX(), (float) player.getY() + 0.02f, (float) player.getZ(), 0, 0, 0, false, 0, Math.PI/2f, 0, 0, 3.5F, 0.83f, 1, 0.39f, 1, 1, 10, true, true, new ParticleComponent[]{
                        new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(1f, 0f), false),
                        new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd(10f, 30f), false)
                });
            }
            if (prevUnderground && !underground) {
                player.playSound(MMSounds.EFFECT_GEOMANCY_BREAK.get(), 1f, 0.9f + random.nextFloat() * 0.1f);
                if (player.level.isClientSide)
                    AdvancedParticleBase.spawnParticle(player.world, ParticleHandler.RING2.get(), (float) player.getX(), (float) player.getY() + 0.02f, (float) player.getZ(), 0, 0, 0, false, 0, Math.PI/2f, 0, 0, 3.5F, 0.83f, 1, 0.39f, 1, 1, 10, true, true, new ParticleComponent[]{
                        new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(1f, 0f), false),
                        new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd(10f, 30f), false)
                });
                player.setDeltaMovement(player.getDeltaMovement().scale(10f));

                for (int i = 0; i < 6; i++) {
                    if (justDug == null) justDug = Blocks.DIRT.defaultBlockState();
//                        ParticleFallingBlock.spawnFallingBlock(player.world, player.getX(), player.getY() + 1, player.getZ(), 30f, 80, 1, player.getRNG().nextFloat() * 0.8f - 0.4f, 0.4f + player.getRNG().nextFloat() * 0.8f, player.getRNG().nextFloat() * 0.8f - 0.4f, ParticleFallingBlock.EnumScaleBehavior.CONSTANT, justDug);
                    EntityFallingBlock fallingBlock = new EntityFallingBlock(EntityHandler.FALLING_BLOCK, player.world, 80, justDug);
                    fallingBlock.setPos(player.getX(), player.getY() + 1, player.getZ());
                    fallingBlock.setDeltaMovement(player.getRNG().nextFloat() * 0.8f - 0.4f, 0.4f + player.getRNG().nextFloat() * 0.8f, player.getRNG().nextFloat() * 0.8f - 0.4f);
                    player.level.addFreshEntity(fallingBlock);
                }
            }
            prevUnderground = underground;
        }

        //Spawning boulder
        if (spawningBoulder) {
            if (player.getDistanceSq(spawnBoulderPos.x(), spawnBoulderPos.y(), spawnBoulderPos.z()) > 36 || !canUse(player)) {
                spawningBoulder = false;
                spawnBoulderCharge = 0;
            }
            else {
                spawnBoulderCharge++;
                if (spawnBoulderCharge > 2) player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 0, 2, false, false));
                if (spawnBoulderCharge == 1 && player.level.isClientSide) MowziesMobs.PROXY.playBoulderChargeSound(player);
                if ((spawnBoulderCharge + 10) % 10 == 0 && spawnBoulderCharge < 40) {
                    if (player.level.isClientSide) {
                        AdvancedParticleBase.spawnParticle(player.world, ParticleHandler.RING2.get(), (float) player.getX(), (float) player.getY() + player.getHeight() / 2f, (float) player.getZ(), 0, 0, 0, false, 0, Math.PI / 2f, 0, 0, 3.5F, 0.83f, 1, 0.39f, 1, 1, 10, true, true, new ParticleComponent[]{
                                new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(0f, 0.7f), false),
                                new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd((0.8f + 2.7f * spawnBoulderCharge / 60f) * 10f, 0), false)
                        });
                    }
                }
                if (spawnBoulderCharge == 50) {
                    if (player.level.isClientSide) {
                        AdvancedParticleBase.spawnParticle(player.world, ParticleHandler.RING2.get(), (float) player.getX(), (float) player.getY() + player.getHeight() / 2f, (float) player.getZ(), 0, 0, 0, true, 0, 0, 0, 0, 3.5F, 0.83f, 1, 0.39f, 1, 1, 20, true, true, new ParticleComponent[]{
                                new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(0.7f, 0f), false),
                                new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd(0, 40f), false)
                        });
                    }
                    player.playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL.get(), 1, 1f);
                }
                if (player.level.isClientSide && spawnBoulderCharge > 5 && spawnBoulderCharge < 30) {
                    int particleCount = 4;
                    while (--particleCount != 0) {
                        double radius = 0.5f + 1.5f * spawnBoulderCharge/30f;
                        double yaw = player.getRNG().nextFloat() * 2 * Math.PI;
                        double pitch = player.getRNG().nextFloat() * 2 * Math.PI;
                        double ox = radius * Math.sin(yaw) * Math.sin(pitch);
                        double oy = radius * Math.cos(pitch);
                        double oz = radius * Math.cos(yaw) * Math.sin(pitch);
                        player.level.addParticle(new ParticleOrb.OrbData((float) player.getX(), (float) player.getY() + player.getHeight() /2f, (float) player.getZ(), 14), player.getX() + ox, player.getY() + oy + player.getHeight()/2, player.getZ() + oz, 0, 0, 0);
                    }
                }
            }
            if (spawnBoulderCharge > 60) {
                spawnBoulder(player);
                liftedMouse = false;
            }
            else {
                int size = (int)Math.min(Math.max(0, Math.floor(spawnBoulderCharge/10.f) - 1), 2) + 1;
                EntityType<EntityBoulder> type = EntityHandler.BOULDERS[size];
                if (!player.world.noCollision(type.getBoundingBoxWithSizeApplied(spawnBoulderPos.x() + 0.5F, spawnBoulderPos.y() + 2, spawnBoulderPos.z() + 0.5F))) {
                    spawnBoulder(player);
                }
            }
        }
//        System.out.println(event.player.tickCount);
    }

    @Override
    public void onRightMouseUp(Player player) {
        super.onRightMouseUp(player);
        liftedMouse = true;
        if (spawningBoulder && player.getDistanceSq(spawnBoulderPos.x(), spawnBoulderPos.y(), spawnBoulderPos.z()) < 36) {
            spawnBoulder(player);
        }
        else {
            spawningBoulder = false;
            spawnBoulderCharge = 0;
        }
    }

    @Override
    public void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        super.onRightClickEmpty(event);
        Player player = event.getPlayer();
        if (event.getHand() == Hand.MAIN_HAND && canUse(player)) {
            if (!tunneling && !spawningBoulder && liftedMouse && spawnBoulderCooldown <= 0) {
                startSpawningBoulder(player);
                MowziesMobs.NETWORK.sendToServer(new MessagePlayerStartSummonBoulder());
            }
        }
    }

    @Override
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        super.onRightClickBlock(event);
        Player player = event.getPlayer();
        if (event.getHand() == Hand.MAIN_HAND && canUse(player)) {
            if (!tunneling && !spawningBoulder && liftedMouse && spawnBoulderCooldown <= 0) {
                startSpawningBoulder(player);
                MowziesMobs.NETWORK.sendToServer(new MessagePlayerStartSummonBoulder());
            }
        }
    }

    @Override
    public void onSneakDown(Player player) {
        super.onSneakDown(player);
        if (doubleTapTimer > 0 && canUse(player) && !player.isOnGround()) {
            tunneling = true;
        }
        doubleTapTimer = 8;
    }

    @Override
    public void onSneakUp(Player player) {
        super.onSneakUp(player);
    }

    public boolean isSpawningBoulder() {
        return spawningBoulder;
    }

    public BlockPos getSpawnBoulderPos() {
        return spawnBoulderPos;
    }

    public Vec3 getLookPos() {
        return lookPos;
    }

    private void spawnBoulder(Player player) {
        int size = (int)Math.min(Math.max(0, Math.floor(spawnBoulderCharge/10.f) - 1), 2);
        if (spawnBoulderCharge >= 60) size = 3;
        EntityBoulder boulder = new EntityBoulder(EntityHandler.BOULDERS[size], player.world, player, spawnBoulderBlock, spawnBoulderPos);
        boulder.setPos(spawnBoulderPos.x() + 0.5F, spawnBoulderPos.y() + 2, spawnBoulderPos.z() + 0.5F);
        if (!player.level.isClientSide && boulder.checkCanSpawn()) {
            player.level.addFreshEntity(boulder);
        }

        if (spawnBoulderCharge > 2) {
            Vec3 playerEyes = player.getEyePosition(1);
            Vec3 vec = playerEyes.subtract(getLookPos()).normalize();
            float yaw = (float) Math.atan2(vec.z, vec.x);
            float pitch = (float) Math.asin(vec.y);
            player.getYRot() = (float) (yaw * 180f / Math.PI + 90);
            player.getXRot() = (float) (pitch * 180f / Math.PI);
        }

        spawnBoulderCooldown = 10;
        spawnBoulderCharge = 0;
        spawningBoulder = false;
    }

    @Override
    public boolean canUse(Player player) {
        return false;//player.getMainHandItem().isEmpty() && player.isPotionActive(EffectHandler.GEOMANCY);
    }

    public int getSpawnBoulderCharge() {
        return spawnBoulderCharge;
    }

    public boolean isBlockDiggable(BlockState blockState) {
        Material mat = blockState.getMaterial();
        if (mat != Material.ORGANIC
                && mat != Material.EARTH
                && mat != Material.ROCK
                && mat != Material.CLAY
                && mat != Material.SAND
                ) {
            return false;
        }
        return blockState.getBlock() != Blocks.HAY_BLOCK
                && blockState.getBlock() != Blocks.NETHER_WART_BLOCK
                && !(blockState.getBlock() instanceof FenceBlock)
                && blockState.getBlock() != Blocks.SPAWNER
                && blockState.getBlock() != Blocks.BONE_BLOCK
                && blockState.getBlock() != Blocks.ENCHANTING_TABLE
                && blockState.getBlock() != Blocks.END_PORTAL_FRAME
                && blockState.getBlock() != Blocks.ENDER_CHEST
                && blockState.getBlock() != Blocks.SLIME_BLOCK
                && blockState.getBlock() != Blocks.HOPPER
                && !blockState.hasTileEntity();
    }

    public void startSpawningBoulder(Player player) {
        Vec3 from = player.getEyePosition(1.0f);
        Vec3 to = from.add(player.getLookVec().scale(SPAWN_BOULDER_REACH));
        BlockHitResult result = player.world.rayTraceBlocks(new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
        if (result.getType() == HitResult.Type.BLOCK) {
            this.lookPos = result.getHitVec();
        }

        this.spawnBoulderPos = result.getPos();
        this.spawnBoulderBlock = player.level.getBlockState(spawnBoulderPos);
        if (result.getFace() != Direction.UP) {
            BlockState blockAbove = player.level.getBlockState(spawnBoulderPos.up());
            //System.out.println(blockAbove.getBlock().getLocalizedName());
            if (blockAbove.isSuffocating(player.world, spawnBoulderPos.up()) || blockAbove.isAir(player.world, spawnBoulderPos.up()))
                return;
        }
        if (!isBlockDiggable(spawnBoulderBlock)) return;
        spawningBoulder = true;
    }
}
