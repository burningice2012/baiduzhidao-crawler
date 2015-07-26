/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

/**
 * annotation to define the sequence
 * 
 * @author BurningIce
 *
 */
public @interface Sequence {
	/**
	 * table name to use
	 * @return
	 */
	String table();
	/**
	 * column name of sequence
	 * @return
	 */
	String column() default "id";
}
