package me.mixces.entityculling.access;

/**
 * Interface for texture managers that support lazy loading
 */
public interface LazyTextureManager {
    /**
     * Enable or disable lazy loading of textures
     */
    void setLazyLoading(boolean lazyLoading);
}
