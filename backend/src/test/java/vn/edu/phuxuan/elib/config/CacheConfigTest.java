package vn.edu.phuxuan.elib.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void defaultCacheConfigurationShouldBeConfigured() {
        RedisCacheConfiguration config = cacheConfig.defaultCacheConfiguration();
        assertNotNull(config);
    }

    @Test
    void redisCacheManagerBuilderCustomizerShouldRegisterCacheNames() {
        RedisCacheConfiguration config = cacheConfig.defaultCacheConfiguration();
        var customizer = cacheConfig.redisCacheManagerBuilderCustomizer(config);
        assertNotNull(customizer);

        RedisCacheManager.RedisCacheManagerBuilder builder = mock(RedisCacheManager.RedisCacheManagerBuilder.class, RETURNS_SELF);
        customizer.customize(builder);

        verify(builder).withCacheConfiguration(eq(CacheConfig.CACHE_ROOT_CATEGORIES), any());
        verify(builder).withCacheConfiguration(eq(CacheConfig.CACHE_CATEGORY_CHILDREN), any());
        verify(builder).withCacheConfiguration(eq(CacheConfig.CACHE_SYSTEM_SETTINGS), any());
        verify(builder).withCacheConfiguration(eq(CacheConfig.CACHE_SYSTEM_SETTINGS_ALL), any());
        verify(builder).withCacheConfiguration(eq(CacheConfig.CACHE_DASHBOARD_SUMMARY), any());
    }
}
