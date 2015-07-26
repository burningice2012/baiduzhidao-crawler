/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author BurningIce
 *
 */
public class CacheableSequenceGeneratorTest {
	private final static String SEQUENCE_NAME = "cacheable-seq-generator-test";
	private final static int THREADS = 30;
	private final static int INVOCATION_COUNT = 10000;
	
	private ZooKeeperSequenceCache sequenceCache;
	private CacheableSequenceGenerator sequenceGenerator;
	private Queue<Long> sequences = new ArrayBlockingQueue<Long>(100000);
	
	@BeforeSuite
	public void setUp() {
		this.sequenceCache = new ZooKeeperSequenceCache();
		this.sequenceCache.setServers("127.0.0.1:2181");
		this.sequenceCache.init();
		this.sequenceCache.clear(SEQUENCE_NAME);
		
		this.sequenceGenerator = new CacheableSequenceGenerator();
		this.sequenceGenerator.setSequenceName(SEQUENCE_NAME);
		this.sequenceGenerator.setInitialValue(1000L);
		this.sequenceGenerator.setCacheSize(1000);
		this.sequenceGenerator.setSequenceCache(sequenceCache);
	}
	
	@Test
	public void testNextValue() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(THREADS);
		for(int i = 0; i < INVOCATION_COUNT; i++) {
			executor.execute(new Runnable() {
				public void run() {
					long value;
					try {
						value = sequenceGenerator.nextValue();
						assertTrue(value >= sequenceGenerator.getInitialValue());
						sequences.offer(Long.valueOf(value));
					} catch (SequenceFailedException e) {
						e.printStackTrace();
					}
				}
			});
		}
		
		executor.shutdown();
		if(!executor.awaitTermination(30, TimeUnit.SECONDS)) {
			executor.shutdownNow();
		}
	}
	
	@AfterSuite
	public void tearDown() {
		this.sequenceCache.destroy();
		Long[] result = this.sequences.toArray(new Long[this.sequences.size()]);
		Arrays.sort(result);
		
		assertEquals(result.length, INVOCATION_COUNT);
		long expectedCurrentValue = result[0].longValue();
		for(int i = 0; i < result.length; ++i) {
			assertEquals(result[i].longValue(), expectedCurrentValue++);
		}
		
		System.out.println("finished");
	}
}
