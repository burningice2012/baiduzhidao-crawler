/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author BurningIce
 *
 */
public class ZooKeeperSequenceCacheCLI {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final ZooKeeperSequenceCache sequenceCache = new ZooKeeperSequenceCache();
		sequenceCache.setServers(args[0]);
		sequenceCache.init();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Bye!");
				sequenceCache.destroy();
			}
		});
		
		for( ; ; ) {
			try {
				String commandText = readLine().trim();
				String[] commandAndArgs = commandText.split("\\s+");
				if(commandAndArgs.length == 0)
					continue;
				
				String command = commandAndArgs[0];
				if("?".equals(command)) {
					printCommands();
				} else if("clear".equalsIgnoreCase(command)) {
					if(commandAndArgs.length < 2) {
						System.out.println("invalid command.");
						continue;
					}
					
					String sequenceName = commandAndArgs[1];
					sequenceCache.clear(sequenceName);
				} else if("incrBy".equalsIgnoreCase(command)) {
					if(commandAndArgs.length < 3) {
						System.out.println("invalid command.");
						continue;
					}
					
					String sequenceName = commandAndArgs[1];
					int increment = Integer.parseInt(commandAndArgs[2]);
					long result = sequenceCache.incrBy(sequenceName, increment);
					System.out.println(result);
				} else if("get".equalsIgnoreCase(command)) {
					if(commandAndArgs.length < 2) {
						System.out.println("invalid command.");
						continue;
					}
					
					String sequenceName = commandAndArgs[1];
					
					long result = sequenceCache.get(sequenceName);
					System.out.println(result);
				} else if("set".equalsIgnoreCase(command)) {
					if(commandAndArgs.length < 3) {
						System.out.println("invalid command.");
						continue;
					}
					
					String sequenceName = commandAndArgs[1];
					int value = Integer.parseInt(commandAndArgs[2]);
					long oldValue = sequenceCache.get(sequenceName);
					long result = sequenceCache.incrBy(sequenceName, (int)(value - oldValue));
					System.out.println("set to " + result + ", old value=" + oldValue);
				} else if("quit".equalsIgnoreCase(command) || "exit".equalsIgnoreCase(command)) {
					break;
				}
			} catch (IOException e) {
				System.out.println("error to read command: " + e.getMessage());
			}
		}

		System.out.println("Bye!");
		sequenceCache.destroy();
	}
	
	private static void printCommands() {
		System.out.println("command list:");
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("clear SEQUENCE_NAME");
		System.out.println("get SEQUENCE_NAME");
		System.out.println("set SEQUENCE_NAME NEW_VALUE");
		System.out.println("incrBy SEQUENCE_NAME INCREMENT");
		System.out.println("exit\n\t#exit the command line.");
		System.out.println("quit\n\t#same as exit.");
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("type any command");
	}

	private static String readLine() throws IOException {
	    while(true) {
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
	    	for(int b = System.in.read() ; b != 10 && b != -1; b = System.in.read()) {
	    		if(b != 13)
	    			baos.write(b);
	    	}
	    	
	    	byte[] bytes = baos.toByteArray();
	    	baos.close();
	    	return new String(bytes);
	    }
	}
}
