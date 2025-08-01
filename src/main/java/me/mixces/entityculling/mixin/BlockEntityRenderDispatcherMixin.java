package me.mixces.entityculling.mixin;

import me.mixces.entityculling.EntityCulling;
import me.mixces.entityculling.handler.CullingHandler;
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
	public void entityCulling$cullBlockEntity(BlockEntity blockEntity, double x, double y, double z, float tickDelta, int blockMiningProgress, CallbackInfo ci) {
		if (!CullingHandler.INSTANCE.shouldCullBlockEntity(blockEntity)) {
			EntityCulling.renderedBlockEntities++;
			return;
		}
		EntityCulling.culledBlockEntities++;
		ci.cancel();
	}
}
