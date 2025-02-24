package dev.tr7zw.entityculling.mixin;

import dev.tr7zw.entityculling.EntityCullingModBase;
import dev.tr7zw.entityculling.access.Cullable;
import dev.tr7zw.entityculling.access.EntityRendererInter;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Shadow
	public abstract <T extends Entity> EntityRenderer<T> getRenderer(Entity entity);

	@Inject(
		method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)Z",
		at = @At("HEAD"),
		cancellable = true
	)
	public void doRenderEntity(Entity entity, double dx, double dy, double dz, float yaw, float tickDelta, boolean skipHitbox, CallbackInfoReturnable<Boolean> cir) {
		Cullable cullable = (Cullable) entity;
		if (true) {
			EntityRendererInter<Entity> entityRenderer = (EntityRendererInter) getRenderer(entity);
			if (entityRenderer.shadowShouldShowName(entity)) {
				entityRenderer.shadowRenderNameTag(entity, dx, dy, dz);
			}
			EntityCullingModBase.instance.skippedEntities++;
			cir.setReturnValue(false);
			return;
		}
		EntityCullingModBase.instance.renderedEntities++;
		cullable.setOutOfCamera(false);
	}
}
