package org.infinispan.quickstart.clusteredcache;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import org.infinispan.Cache;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;

@ApplicationScoped
public class AnyServiceBean implements AnyService {

    @Override
    public void service(Cache<String, String> cache) {
        //System.out.printf("########## Cache contents on node %s\n", cache.getAdvancedCache().getRpcManager().getAddress());
        DistributedExecutorService des = new DefaultExecutorService(cache);
        //Task task = new Task();
        Task task = CDI.current().select(Task.class).get();
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
        System.out.printf("%s -----> totalCount=%d\n\n", builder.toString(), totalCount);
    }
}
