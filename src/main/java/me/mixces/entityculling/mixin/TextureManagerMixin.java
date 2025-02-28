package me.mixces.entityculling.mixin;

import me.mixces.entityculling.access.LazyTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.texture.SimpleTexture;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.resource.manager.ResourceManager;
import net.minecraft.resource.Identifier;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Map;

/**
 * Mixin to implement lazy texture loading in the TextureManager
 */
@Mixin(TextureManager.class)
public abstract class TextureManagerMixin implements LazyTextureManager {

    @Shadow @Final private Map<Identifier, Texture> textures;
    @Shadow private ResourceManager resourceManager;

    @Unique
	private boolean lazyLoading = false;
    @Unique
	private final Map<Identifier, Boolean> loadingTextures = new java.util.concurrent.ConcurrentHashMap<>();
    @Unique
	private final java.util.concurrent.ExecutorService textureLoaderPool =
        java.util.concurrent.Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

    @Override
    public void setLazyLoading(boolean lazyLoading) {
        this.lazyLoading = lazyLoading;
    }

    /**
     * Intercept texture binding to implement lazy loading
     */
    @Inject(method = "bind",
            at = @At("HEAD"),
            cancellable = true)
    public void onBindTexture(Identifier resource, CallbackInfo ci) {
        if (!lazyLoading) {
            return; // Use vanilla behavior
        }

        // Check if texture is already loaded
        Texture textureObject = this.textures.get(resource);

        if (textureObject == null && !loadingTextures.containsKey(resource)) {
            // Need to load the texture
            loadLazily(resource);
            ci.cancel(); // Skip original method as we'll handle it asynchronously
        }
    }

    /**
     * Load texture lazily, potentially asynchronously
     */
	@Unique
    private void loadLazily(Identifier resource) {
        loadingTextures.put(resource, true);

        if (!isEssentialTexture(resource)) {
            // Load non-essential textures asynchronously
            textureLoaderPool.submit(() -> {
                try {
                    // Create a simple texture and load it
                    Texture textureObject = new SimpleTexture(resource);
                    try {
                        textureObject.load(this.resourceManager);
                        this.textures.put(resource, textureObject);
                    } catch (IOException e) {
                        org.apache.logging.log4j.LogManager.getLogger().warn("Failed to load texture: " + resource, e);
                    }
                } finally {
                    loadingTextures.remove(resource);
                }
            });
        } else {
            // Load essential textures immediately
            try {
                Texture textureObject = new SimpleTexture(resource);
                try {
                    textureObject.load(this.resourceManager);
                    this.textures.put(resource, textureObject);
                } catch (IOException e) {
                    org.apache.logging.log4j.LogManager.getLogger().warn("Failed to load texture: " + resource, e);
                }
            } finally {
                loadingTextures.remove(resource);
            }
        }
    }

    /**
     * Determines if a texture is essential and should be loaded immediately
     */
	@Unique
    private boolean isEssentialTexture(Identifier resource) {
        String path = resource.getPath();
        return path.startsWith("textures/gui/") ||
               path.startsWith("textures/font/") ||
               path.equals("textures/misc/unknown_pack.png") ||
               path.equals("textures/gui/options_background.png");
    }

    /**
     * Optimize texture loading to be more selective
     */
    @Inject(method = "register(Lnet/minecraft/resource/Identifier;Lnet/minecraft/client/render/texture/Texture;)Z",
            at = @At("HEAD"),
            cancellable = true)
    public void onload(Identifier resource, Texture textureObject,
                             CallbackInfoReturnable<Texture> cir) {
        if (!lazyLoading || isEssentialTexture(resource)) {
            return; // Use vanilla behavior for essential textures
        }

        if (loadingTextures.containsKey(resource)) {
            // Already being loaded
            cir.setReturnValue(null);
            cir.cancel();
            return;
        }

        // Handle lazy loading
        loadLazily(resource);
        cir.setReturnValue(textureObject);
        cir.cancel();
    }

    /**
     * Inject after class constructor to set up the texture manager
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(ResourceManager resourceManager, CallbackInfo ci) {
        // We'll enable lazy loading later from the Minecraft mixin
    }

    /**
     * Optimize resource reload to be more selective
     */
    @Inject(method = "reload",
            at = @At("HEAD"),
            cancellable = true)
    public void onResourceManagerReload(ResourceManager resourceManager, CallbackInfo ci) {
        if (!lazyLoading) {
            return; // Use vanilla behavior
        }

        // Only reload essential textures immediately
        for (Map.Entry<Identifier, Texture> entry : this.textures.entrySet()) {
            Identifier resource = entry.getKey();

            if (isEssentialTexture(resource)) {
                try {
                    entry.getValue().load(resourceManager);
                } catch (IOException e) {
                    LogManager.getLogger().error("Failed to reload texture: " + resource, e);
                }
            } else {
                // Mark non-essential textures for lazy reloading by removing them
                this.textures.remove(resource);
            }
        }

        ci.cancel(); // Skip the vanilla reload
    }

    /**
     * Shutdown hook called when the game closes
     */
    @Inject(method = "close",
            at = @At("RETURN"))
    public void onShutdown(Identifier textureLocation, CallbackInfo ci) {
        if (!((MinecraftAccessor) Minecraft.getInstance()).getRunning() && textureLoaderPool != null) {
            textureLoaderPool.shutdown();
        }
    }
}
