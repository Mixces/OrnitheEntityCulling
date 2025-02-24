package dev.tr7zw.entityculling.mixin;

import dev.tr7zw.entityculling.EntityCullingModBase;
import dev.tr7zw.entityculling.access.Cullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

	@Inject(
		method = "render(Lnet/minecraft/block/entity/BlockEntity;DDDFI)V",
		at = @At("HEAD"),
		cancellable = true
	)
	public void renderTileEntityAt(BlockEntity blockEntity, double x, double y, double z, float tickDelta, int blockMiningProgress, CallbackInfo ci) {
		if (!((Cullable) blockEntity).isForcedVisible() && ((Cullable) blockEntity).isCulled()) {
			EntityCullingModBase.instance.skippedBlockEntities++;
			ci.cancel();
			return;
		}
		EntityCullingModBase.instance.renderedBlockEntities++;
	}
}
