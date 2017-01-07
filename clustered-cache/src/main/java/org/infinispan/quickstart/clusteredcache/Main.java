package org.infinispan.quickstart.clusteredcache;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class Main {

    public static void main(String[] args) {
        Weld weld = new Weld();
        try (WeldContainer container = weld.initialize()) {
            try {
                container.select(Node.class).get().run();
            } catch (Exception cause) {
                //
            } finally {
            }
        }
    }
}
