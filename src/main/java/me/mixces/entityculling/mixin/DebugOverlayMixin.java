package me.mixces.entityculling.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.mixces.entityculling.EntityCulling;
import net.minecraft.client.gui.overlay.DebugOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(DebugOverlay.class)
public abstract class DebugOverlayMixin {
    @ModifyReturnValue(
		method = "getGameInfo",
		at = @At("RETURN")
	)
    public List<String> entityCulling$addDebugText(List<String> original) {
        original.add(String.format("Rendered Block Entities: %d Culled: %d", EntityCulling.renderedBlockEntities, EntityCulling.culledBlockEntities));
        original.add(String.format("Rendered Entities: %d Culled: %d", EntityCulling.renderedEntities, EntityCulling.culledEntities));
		EntityCulling.resetDebugFields();
		return original;
	}

}
