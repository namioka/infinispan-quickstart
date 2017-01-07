package org.infinispan.quickstart.clusteredcache;

import java.io.Serializable;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.quickstart.clusteredcache.cache.entry.local.LocalCacheEntryManager;

@Dependent
public class Task implements DistributedCallable<String, String, Object[]>, Serializable {

    private Cache<String, String> cache;
    @Inject private LocalCacheEntryManager localCacheEntryManager;

    @Override
    public void setEnvironment(Cache<String, String> cache, Set<String> inputKeys) {
        this.cache = cache;
    }

    @Override
    public Object[] call() throws Exception {
        System.out.printf("this=%s, entryManager=%s\n", this.toString(), this.localCacheEntryManager.toString());
        Object[] result = new Object[2];
        result[0] = this.cache.getCacheManager().getAddress().toString();
        result[1] = this.localCacheEntryManager.primarySegmentsEntries(this.cache).count();
        return result;
    }
}
