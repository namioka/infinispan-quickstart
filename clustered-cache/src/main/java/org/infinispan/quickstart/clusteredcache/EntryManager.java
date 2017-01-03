package org.infinispan.quickstart.clusteredcache;

import java.util.BitSet;
import javax.enterprise.context.ApplicationScoped;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheStream;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.remoting.transport.Address;

@ApplicationScoped
public class EntryManager {

    public <K, V> CacheStream<CacheEntry<K, V>> streamOfLocalPrimarySegmentsEntries(Cache<K, V> cache) {
        
        AdvancedCache<K, V> advancedCache = cache.getAdvancedCache();
        
        DistributionManager distributionManager = advancedCache.getDistributionManager();
        
       // Address localhost = advancedCache.getRpcManager().getAddress();
        Address localhost = cache.getCacheManager().getAddress();
        
        // TODO ch.getMembers().contains(localAddress);
        try {

            ConsistentHash ch = advancedCache.getDistributionManager().getReadConsistentHash();
            
            BitSet primarySegments = new BitSet(ch.getNumSegments());
            ch.getPrimarySegmentsForOwner(localhost).stream()
                    //.peek(x -> {
                    //    System.out.printf("\t address=%s primarySegments=%d\n", localhost, x);
                    //})
                    .forEach(primarySegments::set);
            
            return advancedCache.withFlags(Flag.CACHE_MODE_LOCAL).cacheEntrySet().stream()
                    //.peek(x -> {
                    //    System.out.printf("\t %s -> %s\n", x.getKey(), primarySegments.get(ch.getSegment(x.getKey())));
                    //})
                    .filter(x -> primarySegments.get(ch.getSegment(x.getKey())));

        } catch (IllegalArgumentException cause) {
            
            // TODO
            if (!cause.getMessage().matches("^Node .+ is not a member$")) {
                throw cause;
            }

            System.out.printf("&&&&& %s\n", cause.getMessage());
            throw new RuntimeException(String.format("&&&&& %s is not a member.", localhost));

        } finally {
        }
    }
}
