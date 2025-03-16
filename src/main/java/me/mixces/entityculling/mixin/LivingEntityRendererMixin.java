package me.mixces.entityculling.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

	@WrapOperation(
		method = "render(Lnet/minecraft/entity/living/LivingEntity;DDDFF)V",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableCull()V"
		)
	)
	private void entityCulling$enableCull(Operation<Void> original) {
		GlStateManager.enableCull();
	}

	@WrapOperation(
		method = "render(Lnet/minecraft/entity/living/LivingEntity;DDDFF)V",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/GlStateManager;enableCull()V"
		)
	)
	private void entityCulling$disableCull(Operation<Void> original) {
		GlStateManager.disableCull();
	}

	// TODO/NOTE: Badlion renders mfs as armor stands??
}
