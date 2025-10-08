package ai.dokus.foundation.ktor.cache

import ai.dokus.foundation.ktor.AppConfig
import org.koin.dsl.module

fun redisModule(appConfig: AppConfig, redistNamespace: RedisNamespace) = module {
    single<RedisClient> {
        redis {
            config = appConfig.caching.redis
            namespace = redistNamespace
        }
    }
}