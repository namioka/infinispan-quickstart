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
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Node {

    private static final BasicLogger log = Logger.getLogger(Node.class);
    private volatile boolean stop = false;
    @Inject
    private EmbeddedCacheManagerBean embeddedCacheManagerBean;
    @Inject
    private AnyService anyService;

    public void run() {
        final Cache<String, String> cache = this.embeddedCacheManagerBean.embeddedCacheManager().getCache("dist"/*TODO ENUM?*/);
        this.anyService.service(cache);
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
        try {
            while (System.in.read() > 0) {
                this.anyService.service(cache);
            }
        } catch (IOException cause) {
            throw new RuntimeException(cause);
        }
        this.stop = true;
        try {
            putThread.join();
        } catch (InterruptedException cause) {
            throw new RuntimeException(cause);
        }
        this.embeddedCacheManagerBean.embeddedCacheManager().stop();
    }
}
