package org.infinispan.quickstart.clusteredcache;

import java.io.IOException;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class Main {

    public static void main(String[] args) {
        Weld weld = new Weld();
        try (WeldContainer container = weld.initialize()) {
            container.select(Node.class).get().run();
        } catch (IOException | InterruptedException cause) {
            cause.printStackTrace();
        }
    }
}
