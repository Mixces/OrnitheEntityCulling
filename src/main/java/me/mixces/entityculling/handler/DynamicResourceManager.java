package me.mixces.entityculling.handler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import me.mixces.entityculling.access.LazyTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.Resource;
import net.minecraft.client.resource.manager.ResourceReloadListener;
import net.minecraft.client.resource.manager.SimpleReloadableResourceManager;
import net.minecraft.resource.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Dynamic resource implementation that extends SimpleReloadableResourceManager
 * This allows it to be a drop-in replacement while adding lazy loading functionality
 */
public class DynamicResourceManager extends SimpleReloadableResourceManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // Resource cache with expiration to prevent memory leaks
    private final Cache<Identifier, Resource> resourceCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    // Set to track which domains have been initialized
    private final Set<String> initializedDomains = new java.util.HashSet<>();

    // Flag to indicate whether lazy loading is enabled
    private boolean lazyLoadingEnabled = false;

    // Store whether we're in initialization
    private boolean isInitializing = false;

    public DynamicResourceManager() {
        super(Minecraft.getInstance().getResourcePacks().metadataSerializers);
    }

    /**
     * Initialize lazy loading system
     */
    public void initializeLazyLoading() {
        this.lazyLoadingEnabled = true;

        // Enable lazy loading in the texture manager
        if (Minecraft.getInstance().getTextureManager() instanceof LazyTextureManager) {
            ((LazyTextureManager) Minecraft.getInstance().getTextureManager())
                .setLazyLoading(true);
        }

        // Preload critical resources
        preloadCriticalResources();
    }

    /**
     * Preload only critical resources needed for startup
     */
    private void preloadCriticalResources() {
        isInitializing = true;

        try {
            // Load critical GUI textures
            loadResourceDomain("minecraft", "textures/gui");
            loadResourceDomain("minecraft", "textures/font");

            // Load language files
            loadResourceDomain("minecraft", "lang");
			loadResourceDomain("minecraft", "font");

            // Load sounds.json (needed before any sound plays)
            preloadResource(new Identifier("minecraft", "sounds.json"));
        } catch (Exception e) {
            LOGGER.error("Error preloading critical resources", e);
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Preload a specific resource
     */
    private void preloadResource(Identifier location) {
        try {
            getResource(location);
        } catch (IOException e) {
            LOGGER.debug("Resource not found: " + location);
        }
    }

    /**
     * Load resources in a specific domain and path
     */
    private void loadResourceDomain(String domain, String path) {
        initializedDomains.add(domain);

        // We can't easily enumerate all resources, so we rely on the paths we know exist
        // This is enough to trigger domain initialization
    }

    /**
     * Override the getResource method to implement lazy loading
     */
    @Override
    public Resource getResource(Identifier location) throws IOException {
		String domain = location.getNamespace(); // Get the resource domain (e.g., "minecraft")

		// Check if the domain has been initialized
		if (!initializedDomains.contains(domain)) {
			initializeDomain(domain); // Initialize the domain if it hasn't been
		}

        // Check cache first if lazy loading is enabled
        if (lazyLoadingEnabled && !isInitializing) {
            Resource cachedResource = resourceCache.getIfPresent(location);
            if (cachedResource != null) {
                return cachedResource;
            }
        }

        // Fall back to parent implementation
        Resource resource = super.getResource(location);

        // Cache the resource if lazy loading is enabled
        if (lazyLoadingEnabled && resource != null) {
            resourceCache.put(location, resource);
        }

        return resource;
    }

    /**
     * Perform a more optimized resource refresh
     */
    public void optimizedRefresh() {
        if (!lazyLoadingEnabled) {
            // Use normal refresh if lazy loading is disabled
            Minecraft.getInstance().reloadResources();
            return;
        }

        // Clear caches
        resourceCache.invalidateAll();

        // Reload only critical resources
        preloadCriticalResources();

        // Notify essential reload listeners
        for (ResourceReloadListener listener : getReloadListeners()) {
            if (isEssentialListener(listener)) {
                try {
                    listener.reload(this);
                } catch (Exception e) {
                    LOGGER.error("Failed to call reload listener", e);
                }
            }
        }
    }

    /**
     * Get access to the reload listeners through reflection
     */
    @SuppressWarnings("unchecked")
    private List<ResourceReloadListener> getReloadListeners() {
        try {
            Field field = SimpleReloadableResourceManager.class.getDeclaredField("listeners");
            field.setAccessible(true);
            return (List<ResourceReloadListener>) field.get(this);
        } catch (Exception e) {
            LOGGER.error("Failed to access reload listeners", e);
            return Lists.newArrayList();
        }
    }

    /**
     * Determine if a reload listener is essential and should be called on every refresh
     */
    private boolean isEssentialListener(ResourceReloadListener listener) {
        // These classes need immediate updates
        String className = listener.getClass().getName();
        return className.contains("FontRenderer") ||
               className.contains("LanguageManager") ||
               className.contains("SoundHandler");
    }
}
