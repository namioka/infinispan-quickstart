package org.infinispan.quickstart.clusteredcache.cache.entry.local;

import java.util.BitSet;
import javax.enterprise.context.ApplicationScoped;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheStream;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateTransferManager;
import org.infinispan.topology.CacheTopology;
import org.infinispan.topology.RebalancingStatus;

@ApplicationScoped
public class LocalCacheEntryManager {

    public <K, V> CacheStream<CacheEntry<K, V>> streamOfLocalPrimarySegmentsEntries(Cache<K, V> cache) {
        AdvancedCache<K, V> advancedCache = cache.getAdvancedCache();
        ComponentRegistry componentRegistry = advancedCache.getComponentRegistry();
        StateTransferManager stateTransferManager = componentRegistry.getStateTransferManager();
        CacheTopology cacheTopology = stateTransferManager.getCacheTopology();
        Address localAddress = cache.getCacheManager().getAddress();
        try {
            String rebalancingStatus = null;
            while (!RebalancingStatus.COMPLETE.toString().equals((rebalancingStatus = stateTransferManager.getRebalancingStatus()))) {
                System.out.printf("rebalancingStatus=%s", rebalancingStatus);
                Thread.sleep(1000L);
            }
        } catch (Exception cause) {
            throw new RuntimeException(cause);
        }
        ConsistentHash ch = cacheTopology.getReadConsistentHash();
        while (!ch.getMembers().contains(localAddress)) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException cause) {
                throw new RuntimeException(cause);
            }
        }
        BitSet primarySegments = new BitSet(ch.getNumSegments());
        ch.getPrimarySegmentsForOwner(localAddress).stream()
                //.peek(x -> {
                //    System.out.printf("\t address=%s primarySegments=%d\n", localhost, x);
                //})
                .forEach(primarySegments::set);
        return advancedCache.withFlags(Flag.CACHE_MODE_LOCAL).cacheEntrySet().stream()
                //.peek(x -> {
                //    System.out.printf("\t %s -> %s\n", x.getKey(), primarySegments.get(ch.getSegment(x.getKey())));
                //})
                .filter(x -> primarySegments.get(ch.getSegment(x.getKey())));
    }
}
