package net.crossface22.bettersulfurcubes.mixin;

import net.crossface22.bettersulfurcubes.BscConfig;
import net.crossface22.bettersulfurcubes.BscTags;
import net.crossface22.bettersulfurcubes.access.BscSulfurCubeAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SulfurCube.class)
public abstract class SulfurCubeMixin {

    @Unique
    private SulfurCube bsc$self() {
        return (SulfurCube) (Object) this;
    }

    @Unique
    private BscSulfurCubeAccess bsc$access() {
        return (BscSulfurCubeAccess) this;
    }

    @Inject(method = "playerPush", at = @At("HEAD"), cancellable = true)
    private void bsc$onPlayerPush(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        SulfurCube self = bsc$self();
        ItemStack body = self.getItemBySlot(EquipmentSlot.BODY);
        BscConfig cfg = BscConfig.INSTANCE;

        if (cfg.enableSolid && body.is(BscTags.SOLID)) {
            ci.cancel();
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void bsc$onHurt(ServerLevel serverLevel, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        SulfurCube self = bsc$self();
        ItemStack body = self.getItemBySlot(EquipmentSlot.BODY);

        BscConfig cfg = BscConfig.INSTANCE;

        if (cfg.enableAdhesive && body.is(net.minecraft.world.item.Items.HONEY_BLOCK)) {
            bsc$access().bsc$setStuck(false);
        }

        if (cfg.enableExplosive
                && body.is(net.minecraft.world.item.Items.TNT)
                && source.getDirectEntity() instanceof AbstractArrow arrow
                && arrow.isOnFire()) {
            bsc$access().bsc$explodeAndSplit();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getLightLevelDependentMagicValue", at = @At("HEAD"), cancellable = true)
    private void bsc$onGetLightLevel(CallbackInfoReturnable<Float> cir) {
        SulfurCube self = bsc$self();
        ItemStack body = self.getItemBySlot(EquipmentSlot.BODY);
        if (BscConfig.INSTANCE.enableTwinkle && body.is(BscTags.TWINKLE)) {
            BlockItemStateProperties props = body.getOrDefault(
                    net.minecraft.core.component.DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY);
            Boolean lit = props.get(BlockStateProperties.LIT);
            cir.setReturnValue(lit != null && lit ? 1.0f : 0.0f);
        }
    }
}
