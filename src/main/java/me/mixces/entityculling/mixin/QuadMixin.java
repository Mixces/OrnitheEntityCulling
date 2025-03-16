package me.mixces.entityculling.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.model.Quad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Quad.class)
public class QuadMixin {

    @Unique
    private boolean entityCulling$drawOnSelf;

    @WrapOperation(
		method = "compile",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;begin(ILcom/mojang/blaze3d/vertex/VertexFormat;)V"
		)
	)
    private void entityCulling$beginDraw(BufferBuilder instance, int drawMode, VertexFormat format, Operation<Void> original) {
        entityCulling$drawOnSelf = !((BufferBuilderAccessor) instance).getBuilding();
        if (entityCulling$drawOnSelf) {
			original.call(instance, drawMode, DefaultVertexFormat.POSITION_TEX_NORMAL);
		}
    }

    @WrapOperation(
		method = "compile",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/Tessellator;end()V"
		)
	)
    private void entityCulling$endDraw(Tessellator instance, Operation<Void> original) {
        if (entityCulling$drawOnSelf) {
			original.call(instance);
		}
    }
}
