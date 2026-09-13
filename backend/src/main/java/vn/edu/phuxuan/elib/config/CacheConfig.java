package vn.edu.phuxuan.elib.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ROOT_CATEGORIES = "rootCategories";
    public static final String CACHE_CATEGORY_CHILDREN = "categoryChildren";
    public static final String CACHE_SYSTEM_SETTINGS = "systemSettings";
    public static final String CACHE_SYSTEM_SETTINGS_ALL = "systemSettingsAll";
    public static final String CACHE_DASHBOARD_SUMMARY = "dashboardSummary";

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration defaultCacheConfiguration) {
        return builder -> builder
                .withCacheConfiguration(CACHE_ROOT_CATEGORIES, defaultCacheConfiguration.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_CATEGORY_CHILDREN, defaultCacheConfiguration.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_SYSTEM_SETTINGS, defaultCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CACHE_SYSTEM_SETTINGS_ALL, defaultCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CACHE_DASHBOARD_SUMMARY, defaultCacheConfiguration.entryTtl(Duration.ofMinutes(5)));
    }
}
