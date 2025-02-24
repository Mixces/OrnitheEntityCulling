package dev.tr7zw.entityculling.mixin;

import dev.tr7zw.entityculling.access.EntityRendererInter;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> implements EntityRendererInter<T> {

    @Override
    public boolean shadowShouldShowName(T entity) {
        return shouldRenderNameTag(entity);
    }

    @Override
    public void shadowRenderNameTag(T p_renderName_1_, double p_renderName_2_, double d1, double d2) {
        renderNameTag(p_renderName_1_, p_renderName_2_, d1, d2);
    }

    @Shadow
    protected abstract void renderNameTag(T p_renderName_1_, double p_renderName_2_, double d1, double d2);

    @Shadow
    protected abstract boolean shouldRenderNameTag(T p_canRenderName_1_);

}
