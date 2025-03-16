package me.mixces.entityculling.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tessellator;
import net.minecraft.client.render.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class ModelPartMixin {

    @Shadow
    private boolean compiled;

    @Unique
	private boolean entityCulling$compiledState;

    @Inject(
		method = "render",
		at = @At("HEAD")
	)
    private void entityCulling$resetCompiled(float j, CallbackInfo ci) {
        if (!entityCulling$compiledState) {
            compiled = false;
        }
    }

    @Inject(
		method = "compile",
		at = @At(
			value = "INVOKE_ASSIGN",
			target = "Lcom/mojang/blaze3d/vertex/Tessellator;getBuilder()Lcom/mojang/blaze3d/vertex/BufferBuilder;"
		)
	)
    private void entityCulling$beginRendering(CallbackInfo ci) {
        entityCulling$compiledState = true;
		Tessellator.getInstance().getBuilder().begin(7, DefaultVertexFormat.ENTITY);
    }

    @Inject(
		method = "compile",
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/opengl/GL11;glEndList()V",
			remap = false
		)
	)
    private void entityCulling$draw(CallbackInfo ci) {
		Tessellator.getInstance().end();
    }
}
