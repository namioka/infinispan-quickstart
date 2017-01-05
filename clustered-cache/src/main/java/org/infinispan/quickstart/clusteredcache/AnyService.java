package org.infinispan.quickstart.clusteredcache;

import org.infinispan.Cache;

public interface AnyService {

    void service(Cache<String, String> cache);
}
