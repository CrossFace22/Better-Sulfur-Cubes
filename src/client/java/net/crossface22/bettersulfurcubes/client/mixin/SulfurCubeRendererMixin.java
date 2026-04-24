package net.crossface22.bettersulfurcubes.client.mixin;

import net.crossface22.bettersulfurcubes.client.access.BscBlockModelRenderStateAccess;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SulfurCubeModel;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SulfurCubeRenderer;
import net.minecraft.client.renderer.entity.layers.SulfurCubeInnerLayer;
import net.minecraft.client.renderer.entity.state.SulfurCubeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.renderer.entity.SulfurCubeRenderer.BLOCK_DISPLAY_CONTEXT;

@Mixin(SulfurCubeRenderer.class)
public abstract class SulfurCubeRendererMixin {

    private final BlockModelResolver blockModelResolver;

    public SulfurCubeRendererMixin(final EntityRendererProvider.Context context) {
        super();
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void bsc$makeHoneySolid(SulfurCube entity, SulfurCubeRenderState state, float partialTicks, CallbackInfo ci) {
        ItemStack body = entity.getBodyArmorItem();
        if (body.isEmpty() || state.containedBlock.isEmpty()) return;

        if (body.is(Items.HONEY_BLOCK)) {
            ((BscBlockModelRenderStateAccess) state.containedBlock)
                    .bsc$forceRenderType(RenderTypes.solidMovingBlock());
        }
    }
}
