package me.mixces.entityculling.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.mixces.entityculling.handler.EntityCullingHandler;
import me.mixces.entityculling.handler.TileEntityCullingHandler;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@WrapOperation(
		method = "render(IFJ)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/world/WorldRenderer;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Culler;F)V"
		)
	)
	private void entityCulling$shouldPerformCulling(WorldRenderer instance, Entity camera, Culler culler, float tickDelta, Operation<Void> original) {
		EntityCullingHandler.INSTANCE.setShouldPerformCulling(true);
		TileEntityCullingHandler.INSTANCE.setShouldPerformCulling(true);
		original.call(instance, camera, culler, tickDelta);
		EntityCullingHandler.INSTANCE.setShouldPerformCulling(false);
		TileEntityCullingHandler.INSTANCE.setShouldPerformCulling(false);
	}
}
