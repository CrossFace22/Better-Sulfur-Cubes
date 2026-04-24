package net.crossface22.bettersulfurcubes.mixin;

import net.crossface22.bettersulfurcubes.BscConfig;
import net.crossface22.bettersulfurcubes.BscTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "canBeCollidedWith", at = @At("RETURN"), cancellable = true)
    private void bsc$solidCanBeCollidedWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof SulfurCube self) {
            ItemStack body = self.getItemBySlot(EquipmentSlot.BODY);

            if (BscConfig.INSTANCE.enableSolid && body.is(BscTags.SOLID)) {
                cir.setReturnValue(true);
            }
        }
    }
}
