package me.mixces.entityculling.mixin;

import com.google.common.util.concurrent.ListenableFuture;
import me.mixces.entityculling.handler.CullingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	public abstract ListenableFuture<Object> submit(Runnable event);

	@Shadow
	public ClientWorld world;

	@Inject(
		method = "runGame",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/profiler/Profiler;pop()V",
			ordinal = 3,
			shift = At.Shift.AFTER
		)
	)
	private void entityCulling$renderTickEvent(CallbackInfo ci) {
		if (world != null) {
			submit(CullingHandler.INSTANCE::updateQueries);
//			submit(TileEntityCullingHandler.INSTANCE::updateQueries);
		}
	}

	@Inject(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/profiler/Profiler;pop()V",
			ordinal = 1
		)
	)
	private void entityCulling$clientTickEvent(CallbackInfo ci) {
		if (world != null) {
			CullingHandler.INSTANCE.cleanupQueries();
//			TileEntityCullingHandler.INSTANCE.cleanupQueries();
		}
	}
}
