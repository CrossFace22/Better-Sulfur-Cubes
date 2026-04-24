package net.crossface22.bettersulfurcubes.client.mixin;

import net.crossface22.bettersulfurcubes.client.access.BscBlockModelRenderStateAccess;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockModelRenderState.class)
public abstract class BlockModelRenderStateMixin implements BscBlockModelRenderStateAccess {

    @Accessor("renderType")
    protected abstract void bsc$setRenderType(RenderType type);

    @Override
    @Unique
    public void bsc$forceRenderType(RenderType type) {
        this.bsc$setRenderType(type);
    }
}
