/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

/**
 * @author BurningIce
 *
 */
public interface SequenceGenerator {
	/**
	 * 获取当前Sequence值
	 * @return
	 */
	public long currentValue();
	/**
	 * 获取下一个sequence值
	 * @return
	 */
	public long nextValue() throws SequenceFailedException;
}
