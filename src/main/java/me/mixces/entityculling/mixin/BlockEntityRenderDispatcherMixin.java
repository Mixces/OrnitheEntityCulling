package me.mixces.entityculling.mixin;

import me.mixces.entityculling.handler.CullHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

	@Shadow
	public Entity camera;

	@Shadow
	public World world;

	@Inject(
		method = "render(Lnet/minecraft/block/entity/BlockEntity;DDDFI)V",
		at = @At("HEAD"),
		cancellable = true
	)
	public void entityCulling$cullTileEntity(BlockEntity blockEntity, double x, double y, double z, float tickDelta, int blockMiningProgress, CallbackInfo ci) {
		CullHandler handler = new CullHandler();
		if (handler.shouldRenderBlockEntity(world, camera, blockEntity)) {
			CullHandler.renderedBlockEntities++;
			return;
		}
		CullHandler.culledBlockEntities++;
		ci.cancel();
	}
}
