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
        //AdvancedCache<String, String> advancedCache = cache.getAdvancedCache();
        AdvancedCache<K, V> advancedCache = cache.getAdvancedCache();
        ConsistentHash ch = advancedCache.getDistributionManager().getReadConsistentHash();
        Address localhost = advancedCache.getRpcManager().getAddress();
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
        //.forEach(x -> System.out.printf("\t%s = %s\n", x.getKey(), x.getValue()));
    }
}
