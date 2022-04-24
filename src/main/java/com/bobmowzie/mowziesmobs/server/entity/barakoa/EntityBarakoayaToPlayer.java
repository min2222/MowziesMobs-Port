package com.bobmowzie.mowziesmobs.server.entity.barakoa;

import com.bobmowzie.mowziesmobs.server.ai.NearestAttackableTargetPredicateGoal;
import com.bobmowzie.mowziesmobs.server.potion.EffectHandler;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerLevel;
import net.minecraft.world.level.Level;

public class EntityBarakoayaToPlayer extends EntityBarakoanToPlayer {

    public EntityBarakoayaToPlayer(EntityType<? extends EntityBarakoayaToPlayer> type, Level world) {
        this(type, world, null);
    }

    public EntityBarakoayaToPlayer(EntityType<? extends EntityBarakoayaToPlayer> type, Level world, Player leader) {
        super(type, world, leader);
        setMask(MaskType.FAITH);
        setWeapon(3);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new EntityBarakoaya.HealTargetGoal(this));
    }

    @Override
    protected void registerTargetGoals() {
        super.registerTargetGoals();
        this.targetSelector.addGoal(2, new NearestAttackableTargetPredicateGoal<Player>(this, Player.class, 0, true, true, (new EntityPredicate()).setDistance(getAttributeValue(Attributes.FOLLOW_RANGE)).setCustomPredicate(target -> {
            if (!active) return false;
            if (target != getLeader()) return false;
            return healAICheckTarget(target);
        }).allowFriendlyFire().allowInvulnerable().setSkipAttackChecks().setUseInvisibilityCheck()) {
            @Override
            public boolean shouldContinueExecuting() {
                LivingEntity livingentity = this.goalOwner.getTarget();
                if (livingentity == null) {
                    livingentity = this.target;
                }
                return super.shouldContinueExecuting() && this.goalOwner instanceof EntityBarakoayaToPlayer && ((EntityBarakoayaToPlayer)this.goalOwner).healAICheckTarget(livingentity);
            }

            @Override
            public void startExecuting() {
                targetEntitySelector.setIgnoresLineOfSight().allowInvulnerable().allowFriendlyFire().setSkipAttackChecks().setUseInvisibilityCheck();
                super.startExecuting();
            }
        });
    }

    private boolean healAICheckTarget(LivingEntity livingentity) {
        if (livingentity != getLeader()) return false;
        boolean targetHasTarget = livingentity.getLastAttackedEntity() != null && (livingentity.tickCount - livingentity.getLastAttackedEntityTime() < 120 || livingentity.getDistanceSq(livingentity.getLastAttackedEntity()) < 256);
        if (livingentity.getLastAttackedEntity() instanceof EntityBarakoanToPlayer) targetHasTarget = false;
        boolean canHeal = this.canHeal(livingentity);
        boolean survivalMode = !livingentity.isSpectator() && !((Player)livingentity).isCreative();
        return (livingentity.getHealth() < livingentity.getMaxHealth() || targetHasTarget) && canHeal && survivalMode;
    }

    public boolean canHeal(LivingEntity entity) {
        return entity == leader && entity != null && getDistanceSq(entity) < 256.0;
    }

    @Override
    protected void updateAttackAI() {

    }

    @Override
    protected void sunBlockTarget() {
        LivingEntity target = getTarget();
        if (target != null && target == getLeader()) {
            EffectHandler.addOrCombineEffect(target, EffectHandler.SUNBLOCK, 20, 0, true, false);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(IServerLevel world, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData livingData, CompoundTag compound) {
        setMask(MaskType.FAITH);
        setWeapon(3);
        return super.finalizeSpawn(world, difficulty, reason, livingData, compound);
    }
}
