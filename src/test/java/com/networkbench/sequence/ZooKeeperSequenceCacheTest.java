/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

import static org.testng.Assert.*;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author BurningIce
 *
 */
public class ZooKeeperSequenceCacheTest {
	private ZooKeeperSequenceCache sequenceCache;
	
	@BeforeSuite
	public void setUp() {		
		this.sequenceCache = new ZooKeeperSequenceCache();
		this.sequenceCache.setServers("192.168.2.71:2181");
		this.sequenceCache.init();
		this.sequenceCache.clear("zk-seq-cache-test");
	}
	
	@Test
	public void testIncrBy() {
		assertEquals(this.sequenceCache.incrBy("zk-seq-cache-test", 100), 100);
		assertEquals(this.sequenceCache.incrBy("zk-seq-cache-test", 200), 300);
	}
	
	@AfterSuite
	public void tearDown() {
		this.sequenceCache.destroy();
	}
}
