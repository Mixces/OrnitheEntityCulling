package me.mixces.entityculling.mixin;

import me.mixces.entityculling.handler.CullHandler;
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
	public void entityCulling$cullEntity(Entity entity, double dx, double dy, double dz, float yaw, float tickDelta, boolean skipHitbox, CallbackInfoReturnable<Boolean> cir) {
		if (CullHandler.CULL_RESULTS.get(entity.getNetworkId())) {
			CullHandler.renderedEntities++;
			return;
		}

		/* would be a major disadvantage if we hid nametags lol */
		((EntityRendererAccessor) getRenderer(entity)).invokeRenderNameTag(entity, dx, dy, dz);
		CullHandler.culledEntities++;
		cir.setReturnValue(false);
	}
}
