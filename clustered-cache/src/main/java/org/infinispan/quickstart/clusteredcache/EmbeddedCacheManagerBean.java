package org.infinispan.quickstart.clusteredcache;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

@ApplicationScoped
// @javax.ejb.Singleton
// @javax.ejb.Startup
// @javax.ejb.Lock(javax.ejb.LockType.READ)
public class EmbeddedCacheManagerBean {

    private EmbeddedCacheManager embeddedCacheManager;

    @PostConstruct
    void postConstruct() {
        this.embeddedCacheManager = new DefaultCacheManager(
                GlobalConfigurationBuilder.defaultClusteredBuilder()
                        .transport()
                        .addProperty("configurationFile", "default-configs/default-jgroups-tcp.xml")
                        //.initialClusterSize(3)
                        .build(),
                true
        );
        this.embeddedCacheManager.defineConfiguration("dist"/*TODO ENUM?*/, new ConfigurationBuilder()
                        .clustering()
                        .cacheMode(CacheMode.DIST_SYNC)
                        .hash().numOwners(2)
                        .build()
        );
        this.embeddedCacheManager.startCaches("dist"/*TODO ENUM?*/);
    }

    @PreDestroy
    void preDestroy() {
        this.embeddedCacheManager.stop();
    }

    public EmbeddedCacheManager embeddedCacheManager() {
        return this.embeddedCacheManager;
    }
}
