package me.mixces.entityculling.mixin;

import com.google.common.util.concurrent.ListenableFuture;
import me.mixces.entityculling.handler.DynamicResourceManager;
import me.mixces.entityculling.handler.EntityCullingHandler;
import me.mixces.entityculling.handler.TileEntityCullingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.manager.ReloadableResourceManager;
import net.minecraft.client.resource.manager.SimpleReloadableResourceManager;
import net.minecraft.client.resource.metadata.ResourceMetadataSerializerRegistry;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
			submit(EntityCullingHandler.INSTANCE::updateQueries);
			submit(TileEntityCullingHandler.INSTANCE::updateQueries);
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
			EntityCullingHandler.INSTANCE.cleanupQueries();
			TileEntityCullingHandler.INSTANCE.cleanupQueries();
		}
	}

	@Shadow private ReloadableResourceManager resourceManager;

	/**
	 * Redirects the creation of SimpleReloadableResourceManager to use our implementation instead
	 */
	@Redirect(method = "init",
		at = @At(value = "NEW",
			target = "(Lnet/minecraft/client/resource/metadata/ResourceMetadataSerializerRegistry;)Lnet/minecraft/client/resource/manager/SimpleReloadableResourceManager;"))
	private SimpleReloadableResourceManager createResourceManager(ResourceMetadataSerializerRegistry metadataSerializers) {
		return new DynamicResourceManager();
	}

	/**
	 * Inject after resource packs are loaded to set up lazy loading
	 */
	@Inject(method = "init",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resource/manager/ReloadableResourceManager;addListener(Lnet/minecraft/client/resource/manager/ResourceReloadListener;)V",
			ordinal = 0,
			shift = At.Shift.AFTER))
	private void setupLazyLoading(CallbackInfo ci) {
		if (resourceManager instanceof DynamicResourceManager) {
			((DynamicResourceManager) resourceManager).initializeLazyLoading();
		}
	}

	/**
	 * Prevent unnecessary resource reloading
	 */
	@Inject(method = "reloadResources",
		at = @At("HEAD"),
		cancellable = true)
	private void onRefreshResources(CallbackInfo ci) {
		if (resourceManager instanceof DynamicResourceManager) {
			((DynamicResourceManager) resourceManager).optimizedRefresh();
			ci.cancel(); // Skip vanilla resource reloading
		}
	}
}
