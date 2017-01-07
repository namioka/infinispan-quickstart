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
public class EmbeddedCacheManagerBean {

    private EmbeddedCacheManager embeddedCacheManager;

    @PostConstruct
    void postConstruct() {
        System.out.println("DDDDDDDDDDDDDDDDDDDDDDDDDD");
        this.embeddedCacheManager = new DefaultCacheManager(
                GlobalConfigurationBuilder.defaultClusteredBuilder()
                        .transport()
                        .addProperty("configurationFile", "default-configs/default-jgroups-tcp.xml")
                        .initialClusterSize(3)
                        .build(),
                true
        );
        this.embeddedCacheManager.defineConfiguration("dist"/*TODO ENUM?*/, new ConfigurationBuilder()
                        .clustering()
                        .cacheMode(CacheMode.DIST_SYNC)
                        .hash().numOwners(2)
                        .build()
        );
        System.out.println("EEEEEEEEEEEEEEEEEEEEEEEEEE");
        this.embeddedCacheManager.startCaches("dist"/*TODO ENUM?*/);
        System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFF");
    }

    @PreDestroy
    void preDestroy() {
        System.out.println("GGGGGGGGGGGGGGGGGGGGGGGGGG");
        this.embeddedCacheManager.stop();
        System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHHH");
    }

    // @javax.ejb.Lock(javax.ejb.LockType.READ)
    public EmbeddedCacheManager embeddedCacheManager() {
        return this.embeddedCacheManager;
    }
}
