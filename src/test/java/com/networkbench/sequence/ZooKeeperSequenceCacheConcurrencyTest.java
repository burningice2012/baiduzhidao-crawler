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
public class ZooKeeperSequenceCacheConcurrencyTest {
	public static void main(String[] args) {
		String servers = (args.length == 0 ? "127.0.0.1:2181" : args[0]);
		int concurrency = (args.length > 1 ? Integer.parseInt(args[1]) : 8);
		System.out.println("using zookeeper servers: " + servers + ", concurrency: " + concurrency);
		ZooKeeperSequenceCache sequenceCache = null;
		
		final AtomicBoolean shutdown = new AtomicBoolean(false);
		try {
			sequenceCache = new ZooKeeperSequenceCache();
			sequenceCache.setServers(servers);
			sequenceCache.init();
			//sequenceCache.clear("zk-seq-cache-test");

			final ExecutorService executor = Executors.newFixedThreadPool(concurrency);
			final ZooKeeperSequenceCache sequenceCacheFinal = sequenceCache;
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					if(executor != null) {
						executor.shutdown();
						try {
							if(!executor.awaitTermination(1L, TimeUnit.SECONDS)) {
								executor.shutdownNow();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					if(sequenceCacheFinal != null) {
						sequenceCacheFinal.destroy();
					}
				}
			});
			
			for(int i = 0; i < concurrency; ++i) {
				executor.execute(new Runnable() {
					public void run() {
						for( ; !shutdown.get() ; ) {
							long v = sequenceCacheFinal.incrBy("zk-seq-cache-test", 2);
							if(v == 0L) {
								System.out.println("failed to generate sequence");
							} else {
								System.out.println("sequence: " + v);
							}
							
							try {
								Thread.sleep(100L);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
			
			if(executor != null) {
				executor.shutdown();
				try {
					if(!executor.awaitTermination(30L, TimeUnit.MINUTES)) {
						executor.shutdownNow();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch(Throwable t) {
			t.printStackTrace();
		} finally {
			if(sequenceCache != null)
				sequenceCache.destroy();
		}
	}
	/*
	@Test
	public void testIncrBy() {
		assertEquals(this.sequenceCache.incrBy("redis-seq-cache-test", 100), 100);
		assertEquals(this.sequenceCache.incrBy("redis-seq-cache-test", 200), 300);
	}
	*/
}
