package com.networkbench.sequence;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CacheableSequenceGenerator实现Sequence机制如下：
 * 1. 每次在内存中缓存一段Sequence值（由cacheSize参数控制），用尽后再从SequenceCache（Redis/Memcached）中申请一部分
 * 2. 为保险起见，提供两个SequenceCache，若主SequenceCache失效，则使用备用SequenceCache(secondarySequenceCache)
 * 
 * 
 * /// TODO 实现容灾自动恢复机制
 * @author BurningIce
 *
 */
public class CacheableSequenceGenerator implements SequenceGenerator {
	private SequenceCache sequenceCache;			// 主Cache
	private String sequenceName;					// Sequence name
	private long initialValue;						// 初始值（第一次生成时从初始值开始递增）
	private int cacheSize;							// 每次Cache数量
	private long currentValue;						// 当前值
	private long[] cachedValues;					// 缓存值：[lowerCachedValue, upperCachedValue] upperCachedValue >= currentValue >= lowerCachedValue 
	private Lock lock;
	
	/**
	 * @param sequenceCache the sequenceCache to set
	 */
	public void setSequenceCache(SequenceCache sequenceCache) {
		this.sequenceCache = sequenceCache;
	}

	/**
	 * @return Sequence名称
	 */
	public String getSequenceName() {
		return sequenceName;
	}

	/**
	 * @param sequenceName Sequence名称
	 */
	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	/**
	 * @return 每次sequence缓存大小
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * 每次sequence缓存大小，必须大于0
	 * @param cacheSize the cacheSize to set
	 */
	public void setCacheSize(int cacheSize) {
		this.cacheSize = (cacheSize <= 0 ? 200 : cacheSize);
	}

	/**
	 * @return the initialValue
	 */
	public long getInitialValue() {
		return initialValue;
	}

	/**
	 * @param initialValue the initialValue to set
	 */
	public void setInitialValue(long initialValue) {
		this.initialValue = initialValue;
	}

	
	public CacheableSequenceGenerator() {
		this.initialValue = 1L;
		this.currentValue = 0L;
		this.cachedValues = new long[] {
										0L /* lower bound of cached sequences */, 
										0L /* upper bound of cached sequences */
								};
		this.cacheSize = 200;
		this.lock = new ReentrantLock();
	}

	/* (non-Javadoc)
	 * @see com.networkbench.sequence.SequenceGenerator#currentValue()
	 */
	@Override
	public long currentValue() {
		return this.currentValue;
	}

	@Override
	public long nextValue() throws SequenceFailedException {
		if(this.sequenceCache == null) {
			throw new SequenceFailedException("sequenceCache not provided.");
		}
		
		long nextValue;
		this.lock.lock();
		try {
			if(this.currentValue == 0L) {
				// 初始化，更新缓存
				this.updateCache();
				nextValue = this.currentValue = this.cachedValues[0];
			} else {
				nextValue = ++this.currentValue;
				if(nextValue >= this.cachedValues[1]) {
					// 缓存已用尽，更新缓存
					this.updateCache();
				}
			}
		} finally {
			this.lock.unlock();
		}
		
		return nextValue;
	}

	/**
	 * 缓存已用尽，更新Sequence缓存
	 */
	private void updateCache() throws SequenceFailedException {
		long currentValue = Math.max(this.currentValue, this.initialValue);
		
		long nextCachedValue = this.sequenceCache.incrBy(this.sequenceName, this.cacheSize);
		if(nextCachedValue == 0L) {
			// sequence cache 失效（正常情况下，返回值应  >= this.cachedSize > 0）
			throw new SequenceFailedException("failed to update cache with sequence cache: failed to incrBy(" + this.sequenceName + ", " + this.cacheSize + ")");
		} else if(nextCachedValue < currentValue + this.cacheSize) {
			// 无效的值，可能Sequence之前失效过，例如，主sequenceCache失效，切换至备用sequenceCache上，可能读取到脏数据
			// 需要更新为最新的值（更新时需要考虑分布式环境下的情况）
			// 确保增加后的值 >=  currentValue + this.cacheSize
			long currentCacheValue = nextCachedValue;
			int increment = (int)(currentValue + this.cacheSize - currentCacheValue);
			// 保险起见，防止分布式环境下因Cache失效导致数据不一致，再增加2 * cacheSize
			increment += 2 * this.cacheSize;
			nextCachedValue = this.sequenceCache.incrBy(this.sequenceName, increment);
			if(nextCachedValue == 0L) {
				// sequence cache 失效
				throw new SequenceFailedException("failed to update cache with sequence cache: failed to incrBy(" + this.sequenceName + ", " + this.cacheSize + ")");
			} else {
				this.cachedValues = new long[] {
						nextCachedValue - this.cacheSize + 1	/* lower bound of cached sequences */, 
						nextCachedValue							/* upper bound of cached sequences */
				};
			}
		} else {
			this.cachedValues = new long[] {
					nextCachedValue - this.cacheSize + 1	/* lower bound of cached sequences */, 
					nextCachedValue							/* upper bound of cached sequences */
			};
		}
	}
}
