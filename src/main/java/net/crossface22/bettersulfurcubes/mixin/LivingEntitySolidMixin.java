package net.crossface22.bettersulfurcubes.mixin;

import net.crossface22.bettersulfurcubes.BscConfig;
import net.crossface22.bettersulfurcubes.BscTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySolidMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void bsc$solidNotPushable(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof SulfurCube cube)) return;
        ItemStack body = cube.getItemBySlot(EquipmentSlot.BODY);
        if (BscConfig.INSTANCE.enableSolid && body.is(BscTags.SOLID)) {
            cir.setReturnValue(false);
        }
    }
}
