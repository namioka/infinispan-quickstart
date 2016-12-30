/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.quickstart.clusteredcache;

import java.io.IOException;
import java.util.BitSet;
import javax.enterprise.context.ApplicationScoped;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheStream;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.quickstart.clusteredcache.util.LoggingListener;
import org.infinispan.remoting.transport.Address;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jgroups.util.UUID;

@ApplicationScoped
public class Node {

    private static final BasicLogger log = Logger.getLogger(Node.class);
//    private final boolean useXmlConfig;
//    private final String cacheName;
//    private final String nodeName;
    private volatile boolean stop = false;

    private static final String CACHE_NAME = "dist";

//    public Node(boolean useXmlConfig, String cacheName, String nodeName) {
//        this.useXmlConfig = useXmlConfig;
//        this.cacheName = cacheName;
//        this.nodeName = nodeName;
//    }
//
//    public static void main(String[] args) throws Exception {
//        boolean useXmlConfig = false;
//        String cache = "repl";
//        String nodeName = null;
//        for (String arg : args) {
//            switch (arg) {
//                case "-x":
//                    useXmlConfig = true;
//                    break;
//                case "-p":
//                    useXmlConfig = false;
//                    break;
//                case "-d":
//                    cache = "dist";
//                    break;
//                case "-r":
//                    cache = "repl";
//                    break;
//                default:
//                    nodeName = arg;
//                    break;
//            }
//        }
//        new Node(useXmlConfig, cache, nodeName).run();
//    }
//
    public void run() throws IOException, InterruptedException {
        EmbeddedCacheManager cacheManager = createCacheManager();
        final Cache<String, String> cache = cacheManager.getCache(CACHE_NAME);
        System.out.printf("Cache %s started on %s, cache members are now %s\n", CACHE_NAME, cacheManager.getAddress(),
                cache.getAdvancedCache().getRpcManager().getMembers());
        // Add a listener so that we can see the puts to this node
        cache.addListener(new LoggingListener());
        printCacheContents(cache);
        Thread putThread = new Thread() {
            @Override
            public void run() {
                Address address = cache.getAdvancedCache().getRpcManager().getAddress();
                int counter = 0;
                while (!stop) {
                    try {
                        cache.put(String.format("key-%d", counter), String.format("key-%s-%d", address, counter));
                    } catch (Exception e) {
                        log.warnf("Error inserting key into the cache", e);
                    }
                    counter++;
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
        putThread.start();
        System.out.println("Press Enter to print the cache contents, Ctrl+D/Ctrl+Z to stop.");
        while (System.in.read() > 0) {
            printCacheContents(cache);
        }
        stop = true;
        putThread.join();
        cacheManager.stop();
        System.exit(0);
    }

    /**
     * {@link org.infinispan.Cache#entrySet()}
     */
    private void printCacheContents(Cache<String, String> cache) {
        System.out.printf("Cache contents on node %s\n", cache.getAdvancedCache().getRpcManager().getAddress());
//        ArrayList<Map.Entry<String, String>> entries = new ArrayList<>(cache.entrySet());
//        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
//            @Override
//            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
//                return o1.getKey().compareTo(o2.getKey());
//            }
//        });
//        for (Map.Entry<String, String> e : entries) {
//            System.out.printf("\t%s = %s\n", e.getKey(), e.getValue());
//        }
//        System.out.println();
//        AdvancedCache<String, String> advancedCache = cache.getAdvancedCache();
//        ConsistentHash ch = advancedCache.getDistributionManager().getReadConsistentHash();
//        Address localhost = advancedCache.getRpcManager().getAddress();
//        BitSet primarySegments = new BitSet(ch.getNumSegments());
//        ch.getPrimarySegmentsForOwner(localhost).stream()
//                //.peek(x -> {
//                //    System.out.printf("\t address=%s primarySegments=%d\n", localhost, x);
//                //})
//                .forEach(primarySegments::set);
//
//        advancedCache.withFlags(Flag.CACHE_MODE_LOCAL).cacheEntrySet().stream()
//                //.peek(x -> {
//                //    System.out.printf("\t %s -> %s\n", x.getKey(), primarySegments.get(ch.getSegment(x.getKey())));
//                //})
//                .filter(x -> primarySegments.get(ch.getSegment(x.getKey())))
//                .forEach(x -> System.out.printf("\t%s = %s\n", x.getKey(), x.getValue()));
        this.streamOfLocalPrimarySegmentsEntries(cache)
                .forEach(x -> System.out.printf("\t%s = %s\n", x.getKey(), x.getValue()));
    }

    private <K, V> CacheStream<CacheEntry<K, V>> streamOfLocalPrimarySegmentsEntries(Cache<K, V> cache) {
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

    private EmbeddedCacheManager createCacheManager() throws IOException {
//        if (useXmlConfig) {
//            return createCacheManagerFromXml();
//        } else {
//            return createCacheManagerProgrammatically();
//        }
        return createCacheManagerProgrammatically();
    }

    private EmbeddedCacheManager createCacheManagerProgrammatically() {
        System.out.println("Starting a cache manager with a programmatic configuration");
        DefaultCacheManager cacheManager = new DefaultCacheManager(
                GlobalConfigurationBuilder.defaultClusteredBuilder()
                        .transport().nodeName(UUID.randomUUID().toString()).addProperty("configurationFile", "jgroups.xml")
                        .build(),
                //new ConfigurationBuilder()
                //        .clustering()
                //        .cacheMode(CacheMode.REPL_SYNC)
                //        .build()
                true
        );
        // The only way to get the "repl" cache to be exactly the same as the default cache is to not define it at all
        cacheManager.defineConfiguration(CACHE_NAME, new ConfigurationBuilder()
                .clustering()
                .cacheMode(CacheMode.DIST_SYNC)
                .hash().numOwners(2)
                .build()
        );
        return cacheManager;
    }

//    private EmbeddedCacheManager createCacheManagerFromXml() throws IOException {
//        System.out.println("Starting a cache manager with an XML configuration");
//        System.setProperty("nodeName", nodeName);
//        return new DefaultCacheManager("infinispan.xml");
//    }
}
