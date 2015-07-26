/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

/**
 * @author BurningIce
 *
 */
public interface SequenceCache {
	/**
	 * 清除指定Sequence
	 * @param sequenceName
	 */
	public void clear(String sequenceName);
	/**
	 * 将Cache中的当前Sequence增加一定数值
	 * @param sequenceName Sequence名
	 * @param increment 增量
	 * @return 更新后的Sequence值。若Cache中无对应的squenceName，则先置零，再执行加操作，即返回increment值。
	 */
	public long incrBy(String sequenceName, int increment);
	/**
	 * 获取当前Sequence值
	 * @param sequenceName Sequence名
	 * @return 当前Sequence值
	 */
	public long get(String sequenceName);
}
