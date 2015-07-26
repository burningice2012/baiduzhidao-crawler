/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author BurningIce
 *
 */
public class ZookeeperSequenceGeneratorTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 2) {
			System.out.println("Usage:\n\tjava com.networkbench.sequence ZK_SERVERS SEQUENCE_NAME [THREADS]");
			return;
		}
		
		final AtomicBoolean shutdown = new AtomicBoolean(false);
		final String sequenceName = args[1];
		final int threads = (args.length > 2 ? Integer.parseInt(args[2]) : 1);
		
		final ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			final ZooKeeperSequenceCache sequenceCache = new ZooKeeperSequenceCache();
			sequenceCache.setServers(args[0]);
			sequenceCache.init();
			
			
			final CacheableSequenceGenerator sequenceGenerator = new CacheableSequenceGenerator();
			sequenceGenerator.setSequenceName(sequenceName);
			sequenceGenerator.setInitialValue(1000L);
			sequenceGenerator.setCacheSize(1000);
			sequenceGenerator.setSequenceCache(sequenceCache);
	
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.out.println("Bye!");
					shutdown.set(true);
					sequenceCache.destroy();
					
					executor.shutdown();
					try {
						if(!executor.awaitTermination(5, TimeUnit.SECONDS)) {
							executor.shutdownNow();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			
			for(int i = 0; i < threads; ++i) {
				executor.execute(new Runnable () {
					/* (non-Javadoc)
					 * @see java.lang.Runnable#run()
					 */
					@Override
					public void run() {
						for( ; !shutdown.get(); ) {
							try {
								long v = sequenceGenerator.nextValue();
								System.out.println(v);
								Thread.sleep(5L);
							} catch (SequenceFailedException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

}
