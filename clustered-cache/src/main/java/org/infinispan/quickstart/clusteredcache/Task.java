package org.infinispan.quickstart.clusteredcache;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;

//@Dependent
public class Task implements DistributedCallable<String, String, List<String>>, Serializable {

    private Cache<String, String> cache;
//
//    @Inject
//    private EntryManager entryManager;

    @Override
    public void setEnvironment(Cache<String, String> cache, Set<String> inputKeys) {
        this.cache = cache;
    }

    @Override
    public List<String> call() throws Exception {
        EntryManager entryManager = CDI.current().select(EntryManager.class).get();
        System.out.printf("this=%s, entryManager=%s\n", this.toString(), entryManager.toString());
        return entryManager.streamOfLocalPrimarySegmentsEntries(this.cache)
                .map(x -> x.getValue())
                .collect(Collectors.toList());
//        System.out.printf("this=%s, entryManager=%s\n", this.toString(), this.entryManager.toString());
//        return this.entryManager.streamOfLocalPrimarySegmentsEntries(this.cache)
//                .map(x -> x.getValue())
//                .collect(Collectors.toList());
    }
}
