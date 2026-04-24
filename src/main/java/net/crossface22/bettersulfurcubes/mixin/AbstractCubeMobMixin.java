package net.crossface22.bettersulfurcubes.mixin;

import net.crossface22.bettersulfurcubes.BscConfig;
import net.crossface22.bettersulfurcubes.BscTags;
import net.crossface22.bettersulfurcubes.access.BscSulfurCubeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractCubeMob.class)
public abstract class AbstractCubeMobMixin extends AgeableMob implements BscSulfurCubeAccess {

    protected AbstractCubeMobMixin(EntityType<? extends AgeableMob> type, Level level) {
        super(type, level);
    }

    @Unique private boolean bsc$wasOnGround            = false;
    @Unique private boolean bsc$wasHorizontalCollision = false;
    @Unique private boolean bsc$wasVertCollision       = false;
    @Unique private boolean bsc$isStuck                = false;
    @Unique private boolean bsc$stuckCeiling           = false;
    @Unique private int     bsc$unstuckGrace           = 0;
    @Unique private int     bsc$stuckParticleTimer     = 0;
    @Unique private double  bsc$prevVelX               = 0;
    @Unique private double  bsc$prevVelY               = 0;
    @Unique private double  bsc$prevVelZ               = 0;
    @Unique private BlockPos bsc$prevRedstonePos       = null;

    @Override public boolean bsc$isStuck() { return bsc$isStuck; }

    @Override
    public void bsc$setStuck(boolean v) {
        if (bsc$isStuck == v) return;
        bsc$isStuck = v;
        if (!v) {
            bsc$unstuckGrace       = 40;
            bsc$stuckCeiling       = false;
            bsc$stuckParticleTimer = 0;
            setNoGravity(false);
        }
    }

    @Unique
    private AbstractCubeMob bsc$self() {
        return (AbstractCubeMob) (Object) this;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void bsc$captureVelocity(CallbackInfo ci) {
        if (!((Object) this instanceof SulfurCube)) return;
        if (level().isClientSide()) return;
        Vec3 vel = getDeltaMovement();
        bsc$prevVelX = vel.x;
        bsc$prevVelY = vel.y;
        bsc$prevVelZ = vel.z;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bsc$onTick(CallbackInfo ci) {
        if (!((Object) this instanceof SulfurCube)) return;

        AbstractCubeMob mob = bsc$self();

        if (mob.level().isClientSide()) {
            bsc$wasOnGround = mob.onGround();
            return;
        }

        ItemStack body        = mob.getItemBySlot(EquipmentSlot.BODY);
        boolean justLanded    = mob.onGround() && !bsc$wasOnGround;
        boolean hitWall       = mob.horizontalCollision;
        boolean justHitWall   = hitWall && !bsc$wasHorizontalCollision;
        boolean hitCeiling    = mob.verticalCollision && !mob.onGround() && bsc$prevVelY > 0.05;
        boolean justHitCeiling = hitCeiling && !bsc$wasVertCollision;

        BscConfig cfg = BscConfig.INSTANCE;

        if (bsc$prevRedstonePos != null
                && (body.isEmpty() || !cfg.enableRedstone || !body.is(BscTags.REDSTONE))) {
            if (mob.level() instanceof ServerLevel sl) {
                sl.updateNeighborsAt(bsc$prevRedstonePos, Blocks.REDSTONE_BLOCK);
            }
            bsc$prevRedstonePos = null;
        }

        if (!body.isEmpty()) {
            if      (cfg.enableAdhesive  && body.is(Items.HONEY_BLOCK)) bsc$tickAdhesive(hitWall, justHitCeiling);
            else if (cfg.enableExplosive && body.is(Items.TNT)) bsc$tickExplosive(justLanded, hitWall);
            else if (cfg.enableMusical   && body.is(Items.NOTE_BLOCK)) bsc$tickMusical(justLanded, justHitWall, justHitCeiling);
            else if (cfg.enableTwinkle   && body.is(BscTags.TWINKLE)) bsc$tickTwinkle(justLanded, hitWall);
            else if (cfg.enableRedstone  && body.is(BscTags.REDSTONE)) bsc$tickRedstone();
        } else {
            if (bsc$isStuck) bsc$setStuck(false);
        }

        bsc$wasOnGround            = mob.onGround();
        bsc$wasHorizontalCollision = hitWall;
        bsc$wasVertCollision       = mob.verticalCollision && !mob.onGround();
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"))
    private void bsc$painfulOnPush(Entity other, CallbackInfo ci) {
        if (!((Object) this instanceof SulfurCube cube)) return;

        double movementThreshold = 0.05D;
        if (cube.getDeltaMovement().lengthSqr() < movementThreshold) return;

        ItemStack body = cube.getItemBySlot(EquipmentSlot.BODY);
        if (BscConfig.INSTANCE.enablePainful && body.is(BscTags.PAINFUL)
                && other instanceof LivingEntity target && !(other instanceof Player)
                && cube.level() instanceof ServerLevel sl) {
            target.hurtServer(sl, sl.damageSources().mobAttack(cube), 1.0f);
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true)
    private void bsc$solidCancelPush(Entity other, CallbackInfo ci) {
        if (!((Object) this instanceof SulfurCube cube)) return;
        ItemStack body = cube.getItemBySlot(EquipmentSlot.BODY);
        if (BscConfig.INSTANCE.enableSolid && body.is(BscTags.SOLID)) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void bsc$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!((Object) this instanceof SulfurCube)) return;
        if (bsc$prevRedstonePos != null && bsc$self().level() instanceof ServerLevel sl) {
            sl.updateNeighborsAt(bsc$prevRedstonePos, Blocks.REDSTONE_BLOCK);
            bsc$prevRedstonePos = null;
        }
    }

    // EXPLOSIVE

    @Unique
    private void bsc$tickExplosive(boolean justLanded, boolean hitWall) {
        AbstractCubeMob mob = bsc$self();

        boolean fastWallHit   = hitWall    && (Math.abs(bsc$prevVelX) + Math.abs(bsc$prevVelZ)) > 0.35;
        boolean fastGroundHit = justLanded && Math.abs(bsc$prevVelY) > 0.45;
        boolean onFireOrLava  = mob.isOnFire() || mob.isInLava();

        if (fastWallHit || fastGroundHit || onFireOrLava) {
            bsc$explodeAndSplit();
        }
    }

    @Override
    public void bsc$explodeAndSplit() {
        AbstractCubeMob mob = bsc$self();
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        serverLevel.explode(mob,
                mob.getX(), mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(),
                3.0f, Level.ExplosionInteraction.TNT);

        int currentSize = mob.getSize();
        if (currentSize > 1) {
            int smallerSize  = currentSize / 2;
            float startAngle = mob.getRandom().nextFloat() * (float) (Math.PI * 2);
            double spread    = mob.getBbWidth() * 0.4;

            for (int i = 0; i < 2; i++) {
                double angle = startAngle + i * Math.PI;
                AbstractCubeMob child = (AbstractCubeMob) mob.getType().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
                if (child == null) continue;

                child.setSize(smallerSize, true);
                child.setBaby(true);
                child.snapTo(
                        mob.getX() + Math.cos(angle) * spread,
                        mob.getY() + 0.5,
                        mob.getZ() + Math.sin(angle) * spread,
                        mob.getRandom().nextFloat() * 360f, 0f);
                child.setDeltaMovement(
                        Math.cos(angle) * 0.35,
                        0.3,
                        Math.sin(angle) * 0.35);
                serverLevel.addFreshEntity(child);
            }
        }

        mob.discard();
    }

    // MUSICAL

    @Unique
    private void bsc$tickMusical(boolean justLanded, boolean hitWall, boolean justHitCeiling) {
        if (!justLanded && !hitWall && !justHitCeiling) return;
        AbstractCubeMob mob = bsc$self();

        BlockState impactBlock;
        if (justLanded) {
            impactBlock = bsc$findGroundBlock(mob);
        } else if (justHitCeiling) {
            impactBlock = bsc$findCeilingBlock(mob);
        } else {
            impactBlock = bsc$findWallBlock(mob);
        }

        if (impactBlock.isAir()) return;

        NoteBlockInstrument instrument = impactBlock.instrument();
        float pitch = (float) Math.pow(2.0, (mob.getRandom().nextInt(25) - 12) / 12.0);

        mob.level().playSound(null,
                mob.getX(), mob.getY(), mob.getZ(),
                instrument.getSoundEvent().value(),
                SoundSource.RECORDS, 3.0f, pitch);

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.NOTE,
                    mob.getX(), mob.getY() + mob.getBbHeight() + 0.5, mob.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Unique
    private BlockState bsc$findGroundBlock(AbstractCubeMob mob) {
        double cx    = mob.getX();
        double cz    = mob.getZ();
        double hw    = mob.getBbWidth() / 2.0;
        double scanY = mob.getY() - 0.1;
        Level  level = mob.level();

        double[] offsets = {0.0, hw * 0.75, -hw * 0.75};
        for (double ox : offsets) {
            for (double oz : offsets) {
                BlockState s = level.getBlockState(BlockPos.containing(cx + ox, scanY, cz + oz));
                if (!s.isAir()) return s;
            }
        }
        return level.getBlockState(BlockPos.containing(cx, scanY, cz));
    }

    @Unique
    private BlockState bsc$findCeilingBlock(AbstractCubeMob mob) {
        double cx    = mob.getX();
        double cz    = mob.getZ();
        double hw    = mob.getBbWidth() / 2.0;
        double scanY = mob.getY() + mob.getBbHeight() + 0.1;
        Level  level = mob.level();

        double[] offsets = {0.0, hw * 0.75, -hw * 0.75};
        for (double ox : offsets) {
            for (double oz : offsets) {
                BlockState s = level.getBlockState(BlockPos.containing(cx + ox, scanY, cz + oz));
                if (!s.isAir()) return s;
            }
        }
        return level.getBlockState(BlockPos.containing(cx, scanY, cz));
    }

    @Unique
    private BlockState bsc$findWallBlock(AbstractCubeMob mob) {
        double cx    = mob.getX();
        double cz    = mob.getZ();
        double hw    = mob.getBbWidth() / 2.0;
        double baseY = mob.getY();
        double bbH   = mob.getBbHeight();
        Level  level = mob.level();

        double[] ys   = {baseY + bbH * 0.5, baseY + bbH * 0.2, baseY + bbH * 0.8, baseY - 0.4, baseY + bbH + 0.4};
        double[] perp = {0.0, hw * 0.75, -hw * 0.75};
        double d      = 0.05;
        double[][] faces = bsc$buildFaceOrder(hw + d);

        for (double[] face : faces) {
            boolean isXFace = face[0] != 0;
            for (double y : ys) {
                for (double p : perp) {
                    double scanX = cx + face[0] + (isXFace ? 0 : p);
                    double scanZ = cz + face[1] + (isXFace ? p : 0);
                    BlockState state = level.getBlockState(BlockPos.containing(scanX, y, scanZ));
                    if (!state.isAir()) return state;
                }
            }
        }
        return level.getBlockState(BlockPos.containing(cx, baseY - 0.1, cz));
    }

    @Unique
    private double[][] bsc$buildFaceOrder(double reach) {
        double ax = Math.abs(bsc$prevVelX);
        double az = Math.abs(bsc$prevVelZ);

        if (ax >= az) {
            return new double[][]{
                {bsc$prevVelX >= 0 ?  reach : -reach, 0},
                {bsc$prevVelX >= 0 ? -reach :  reach, 0},
                {0,  reach}, {0, -reach}
            };
        } else {
            return new double[][]{
                {0, bsc$prevVelZ >= 0 ?  reach : -reach},
                {0, bsc$prevVelZ >= 0 ? -reach :  reach},
                { reach, 0}, {-reach, 0}
            };
        }
    }

    // ADHESIVE

    @Unique
    private void bsc$tickAdhesive(boolean hitWall, boolean justHitCeiling) {
        AbstractCubeMob mob = bsc$self();

        if (bsc$isStuck) {
            if (mob.isLeashed()
                    || mob.isPassenger()
                    || bsc$isTouchedByLiving(mob)
                    || bsc$isFished(mob)
                    || !bsc$hasSupport(mob)) {
                bsc$setStuck(false);
                return;
            }

            setNoGravity(true);
            if (mob.getDeltaMovement().lengthSqr() > 1e-10) {
                mob.setDeltaMovement(Vec3.ZERO);
            }

            if (mob.level() instanceof ServerLevel serverLevel) {
                bsc$stuckParticleTimer--;
                if (bsc$stuckParticleTimer <= 0) {
                    bsc$stuckParticleTimer = 120;
                    bsc$emitStuckHoneyParticles(serverLevel, mob);
                }
            }
            return;
        }

        if (bsc$unstuckGrace > 0) {
            bsc$unstuckGrace--;
            return;
        }

        if (hitWall || justHitCeiling) {
            bsc$isStuck      = true;
            bsc$stuckCeiling = justHitCeiling && !hitWall;
            setNoGravity(true);
            mob.setDeltaMovement(Vec3.ZERO);
            mob.level().playSound(null,
                    mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.HONEY_BLOCK_PLACE, SoundSource.NEUTRAL, 1.0f, 0.8f);
            if (mob.level() instanceof ServerLevel serverLevel) {
                bsc$stuckParticleTimer = 40;
                bsc$emitStuckHoneyParticles(serverLevel, mob);
            }
        }
    }

    @Unique
    private void bsc$emitStuckHoneyParticles(ServerLevel level, AbstractCubeMob mob) {
        AABB bb  = mob.getBoundingBox();
        double w = mob.getBbWidth() * 0.4;
        int count = Math.max(3, (int)(mob.getBbWidth() * mob.getBbWidth() * 6));
        if (bsc$stuckCeiling) {
            level.sendParticles(ParticleTypes.FALLING_HONEY,
                    mob.getX(), bb.maxY + 0.05, mob.getZ(),
                    count, w, 0.02, w, 0.0);
        } else {
            level.sendParticles(ParticleTypes.FALLING_HONEY,
                    mob.getX(), bb.minY + 0.1, mob.getZ(),
                    count, w, 0.05, w, 0.01);
        }
    }

    @Unique
    private boolean bsc$isTouchedByLiving(AbstractCubeMob mob) {
        return !mob.level().getEntities(mob, mob.getBoundingBox(),
                e -> e instanceof LivingEntity && !(e instanceof SulfurCube)).isEmpty();
    }

    @Unique
    private boolean bsc$isFished(AbstractCubeMob mob) {
        return !mob.level().getEntities(mob, mob.getBoundingBox().inflate(1.0),
                e -> e instanceof FishingHook fh && mob.equals(fh.getHookedIn())).isEmpty();
    }

    @Unique
    private boolean bsc$hasSupport(AbstractCubeMob mob) {
        Level level = mob.level();
        BlockPos center = mob.blockPosition();
        if (bsc$stuckCeiling) {
            return level.getBlockState(center.above()).isSolid()
                    || level.getBlockState(center.above().above()).isSolid();
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(center.relative(dir)).isSolid()) return true;
            if (level.getBlockState(center.above().relative(dir)).isSolid()) return true;
        }
        return false;
    }

    // TWINKLE

    @Unique
    private void bsc$tickTwinkle(boolean justLanded, boolean hitWall) {
        if (!justLanded && !hitWall) return;
        AbstractCubeMob mob  = bsc$self();
        ItemStack body       = mob.getItemBySlot(EquipmentSlot.BODY);
        BlockItemStateProperties props = body.getOrDefault(
                net.minecraft.core.component.DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY);
        Boolean wasLit = props.get(BlockStateProperties.LIT);
        boolean newLit = wasLit == null || !wasLit;
        body.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                props.with(BlockStateProperties.LIT, newLit));
    }

    // REDSTONE

    @Unique
    private void bsc$tickRedstone() {
        AbstractCubeMob mob = bsc$self();
        if (!(mob.level() instanceof ServerLevel sl)) return;

        BlockPos current = mob.blockPosition();
        if (!current.equals(bsc$prevRedstonePos)) {
            if (bsc$prevRedstonePos != null) {
                sl.updateNeighborsAt(bsc$prevRedstonePos, Blocks.REDSTONE_BLOCK);
            }
            sl.updateNeighborsAt(current, Blocks.REDSTONE_BLOCK);
            bsc$prevRedstonePos = current;
        }
    }
}
