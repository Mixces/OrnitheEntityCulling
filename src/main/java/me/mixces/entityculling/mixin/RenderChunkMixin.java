package me.mixces.entityculling.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.world.CompiledChunk;
import net.minecraft.client.render.world.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderChunk.class)
public class RenderChunkMixin {
	@WrapOperation(
		method = "compile",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/world/CompiledChunk;addBlockEntity(Lnet/minecraft/block/entity/BlockEntity;)V"
		)
	)
	private void entityCulling$onlyRenderBlockEntityOnce(CompiledChunk instance, BlockEntity blockEntity, Operation<Void> original) {
		if (!BlockEntityRenderDispatcher.INSTANCE.getRenderer(blockEntity).shouldRenderOffScreen()) {
			original.call(instance, blockEntity);
		}
	}
}
