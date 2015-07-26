/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.atomic.PromotedToLock;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.retry.RetryUntilElapsed;

/**
 * @author BurningIce
 *
 */
public class ZooKeeperSequenceCache implements SequenceCache {
	private final static Log logger = LogFactory.getLog(ZooKeeperSequenceCache.class);
	private final static int DEFAULT_ZK_CONNECTION_TIMEOUT = 3000;
	private final static String ZNODE_SEQUENCES_NAMESPACE = "ZK_SEQUENCES";
	private final static int ZK_RETRY_INTERVAL = 500;
	private CuratorFramework zkClient;
	private String servers;
	private int connectionTimeout = DEFAULT_ZK_CONNECTION_TIMEOUT;
	private ReentrantLock localLock;	// 防止本地多线程并发
	private Map<String /* sequenceName */, DistributedAtomicLong> sequences;
	
	/**
	 * @param servers the servers to set
	 */
	public void setServers(String servers) {
		this.servers = servers;
	}

	/**
	 * @param connectionTimeout the connectionTimeout to set
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public ZooKeeperSequenceCache() {
		this.sequences = new HashMap<String, DistributedAtomicLong>();
		this.localLock = new ReentrantLock();
	}
	
	public void init() {
		this.zkClient = CuratorFrameworkFactory.builder()
							.connectString(this.servers)
							.namespace(ZNODE_SEQUENCES_NAMESPACE)
							.retryPolicy(new RetryUntilElapsed(this.connectionTimeout, ZK_RETRY_INTERVAL))
							.connectionTimeoutMs(this.connectionTimeout)
							.build();
		this.zkClient.start();
	}
	
	/* (non-Javadoc)
	 * @see com.networkbench.sequence.SequenceCache#clear(java.lang.String)
	 */
	@Override
	public void clear(String sequenceName) {
		DistributedAtomicLong sequence = createSequenceIfNotExists(sequenceName);
		try {
			if(sequence != null) {
				// sequence.forceSet(0L);
				this.zkClient.delete().forPath("/" + sequenceName);
				synchronized (this) {
					this.sequences.remove(sequenceName);
				}
			}
		} catch (Exception e) {
			logger.error("failed to clear sequence: " + sequenceName, e);
		}
	}

	/* (non-Javadoc)
	 * @see com.networkbench.sequence.SequenceCache#get(java.lang.String)
	 */
	@Override
	public long get(String sequenceName) {
		DistributedAtomicLong sequence = createSequenceIfNotExists(sequenceName);
		long value = 0L;
		
		if(sequence != null) {
			AtomicValue<Long> v;
			this.localLock.lock();
			try {
				v = sequence.get();
				if(v != null && v.succeeded()) {
					value = v.postValue();
				} else {
					logger.error("failed to get sequence: " + sequenceName + (v == null ? ": null" : ", succeeded: false"));
				}
			} catch (Exception e) {
				logger.error("failed to get sequence: " + sequenceName, e);
			} finally {
				this.localLock.unlock();
			}
		}
		
		return value;
	}

	/* (non-Javadoc)
	 * @see com.networkbench.sequence.SequenceCache#incrBy(java.lang.String, int)
	 */
	@Override
	public long incrBy(String sequenceName, int increment) {
		DistributedAtomicLong sequence = createSequenceIfNotExists(sequenceName);
		long valueWithIncrement = 0L;
		
		if(sequence != null) {
			AtomicValue<Long> v;
			this.localLock.lock();
			try {
				v = sequence.add(Long.valueOf(increment));
				if(v != null && v.succeeded()) {
					valueWithIncrement = v.postValue();
				} else {
					logger.error("failed to incrBy sequence: " + sequenceName + (v == null ? ": null" : ", succeeded: false"));
				}
			} catch (Exception e) {
				logger.error("failed to incrBy sequence: " + sequenceName, e);
			} finally {
				this.localLock.unlock();
			}
		}
		
		return valueWithIncrement;
	}
	
	private DistributedAtomicLong createSequenceIfNotExists(String sequenceName) {
		DistributedAtomicLong sequence = this.sequences.get(sequenceName);
		if(sequence == null) {
			this.localLock.lock();
			try {
				sequence = this.sequences.get(sequenceName);
				if(sequence == null) {
					sequence = new DistributedAtomicLong(
											zkClient, "/" + sequenceName, 
											new RetryOneTime(ZK_RETRY_INTERVAL)
											/*, 
											PromotedToLock.builder().lockPath("/locks-" + sequenceName)
																	.retryPolicy(new RetryNTimes(5, ZK_RETRY_INTERVAL))
																	.timeout(ZK_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)
																	.build()*/);
					this.sequences.put(sequenceName, sequence);
				}
			} finally {
				this.localLock.unlock();
			}
		}
		
		return sequence;
	}

	public void destroy() {
		if(this.zkClient != null) {
			this.zkClient.close();
		}
	}
}
