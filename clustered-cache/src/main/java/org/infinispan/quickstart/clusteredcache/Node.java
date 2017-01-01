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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Node {

    private static final BasicLogger log = Logger.getLogger(Node.class);
    private volatile boolean stop = false;
    private static final String CACHE_NAME = "dist";

    private EmbeddedCacheManager embeddedCacheManager;

    @Inject
    private EntryManager entryManager;

    @PostConstruct
    void postConstruct() {
        long s;
        System.out.printf("########## Start create embeddedCacheManager\n");
        s = System.currentTimeMillis();
        this.embeddedCacheManager = new DefaultCacheManager(
                GlobalConfigurationBuilder.defaultClusteredBuilder()
                        .transport()
                        .addProperty("configurationFile", "default-configs/default-jgroups-tcp.xml")
                        //.initialClusterSize(3)
                        .build(),
                true
        );
        this.embeddedCacheManager.defineConfiguration(CACHE_NAME, new ConfigurationBuilder()
                .clustering()
                .cacheMode(CacheMode.DIST_SYNC)
                .hash().numOwners(2)
                .build()
        );
        System.out.printf("########## End create embeddedCacheManager (%d)\n", (System.currentTimeMillis() - s));
        System.out.printf("########## Start startCaches\n");
        s = System.currentTimeMillis();
        this.embeddedCacheManager.startCaches(CACHE_NAME);
        System.out.printf("########## End startCaches (%d)\n", (System.currentTimeMillis() - s));
    }

    public void run() throws IOException, InterruptedException {
        final Cache<String, String> cache = this.embeddedCacheManager.getCache(CACHE_NAME);
//        System.out.printf("Cache %s started on %s, cache members are now %s\n",
//                CACHE_NAME,
//                this.embeddedCacheManager.getAddress(),
//                cache.getAdvancedCache().getRpcManager().getMembers());
        // cache.addListener(new LoggingListener());
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
                        Thread.sleep(1L);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
        putThread.start();
        System.out.println("########## Press Enter to print the cache contents, Ctrl+D/Ctrl+Z to stop.");
        while (System.in.read() > 0) {
            printCacheContents(cache);
        }
        this.stop = true;
        putThread.join();
        this.embeddedCacheManager.stop();
    }

    private void printCacheContents(Cache<String, String> cache) {
        System.out.printf("########## Cache contents on node %s\n", cache.getAdvancedCache().getRpcManager().getAddress());
        List<String> localEntries = this.entryManager.streamOfLocalPrimarySegmentsEntries(cache)
                .map(x -> String.format("\t%s = %s\n", x.getKey(), x.getValue()))
                .collect(Collectors.toList());
        DistributedExecutorService des = new DefaultExecutorService(cache);

        Task task = new Task();
        //Task task = CDI.current().select(Task.class).get();

        StringBuilder builder = new StringBuilder();
        long totalCount = 0;
        List<CompletableFuture<Object[]>> futures = des.submitEverywhere(task);
        try {
            for (CompletableFuture<Object[]> future : futures) {
                Object[] result = future.get();
                builder.append(String.format("node=%s, count=%d ", result[0], (long) result[1]));
                totalCount += (long) result[1];
            }
        } catch (InterruptedException | ExecutionException cause) {
            //throw new RuntimeException(cause);
            cause.printStackTrace();
            //System.out.printf("@@@@@ %s, %s\n", cause.getMessage(), cause.getCause().getMessage());
            // IGNORE.
        }
        System.out.printf("%d, %s -----> totalCount=%d\n\n\n", localEntries.size(), builder.toString(), totalCount);
    }
}
