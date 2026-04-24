package net.crossface22.bettersulfurcubes.mixin;

import net.crossface22.bettersulfurcubes.BscConfig;
import net.crossface22.bettersulfurcubes.BscTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SignalGetter.class)
public interface LevelRedstoneSignalMixin {

    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
    private void bsc$injectRedstoneSignal(BlockPos pos, Direction dir,
                                           CallbackInfoReturnable<Integer> cir) {
        if (!BscConfig.INSTANCE.enableRedstone) return;
        if (!(this instanceof Level level)) return;
        if (level.isClientSide()) return;

        AABB box = new AABB(
                pos.getX(),     pos.getY(),     pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

        List<SulfurCube> cubes = level.getEntitiesOfClass(
                SulfurCube.class, box,
                cube -> {
                    if (cube.isRemoved()) return false;
                    if (!cube.blockPosition().equals(pos)) return false;
                    ItemStack body = cube.getItemBySlot(EquipmentSlot.BODY);
                    return !body.isEmpty() && body.is(BscTags.REDSTONE);
                });

        if (!cubes.isEmpty()) {
            cir.setReturnValue(15);
        }
    }
}
