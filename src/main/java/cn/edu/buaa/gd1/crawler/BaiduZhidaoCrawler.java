/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package cn.edu.buaa.gd1.crawler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.networkbench.sequence.CacheableSequenceGenerator;
import com.networkbench.sequence.ZooKeeperSequenceCache;

/**
 * @author BurningIce
 *
 */
public class BaiduZhidaoCrawler {
	private final static String SEQUENCE_NAME_BAIDU_ZHIDAO_QUESTION_ID = "SEQ_BAIDU_ZHIDAO_QID";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int threads = getSystemPropertyValueAsInt("threads", 8);					// 并发线程数
		String zkServers = System.getProperty("zkServers");
		if(zkServers == null) {
			System.out.println("zkServers not configured in properties");
			return;
		}
		
		String savePath = System.getProperty("savePath", ".");
		int sleepTime = getSystemPropertyValueAsInt("sleepTime", 500);

		final ZooKeeperSequenceCache sequenceCache = new ZooKeeperSequenceCache();
		sequenceCache.setServers(zkServers);
		sequenceCache.init();
		
		CacheableSequenceGenerator sequenceGenerator = new CacheableSequenceGenerator();
		sequenceGenerator.setSequenceName(SEQUENCE_NAME_BAIDU_ZHIDAO_QUESTION_ID);
		sequenceGenerator.setInitialValue(100L);
		sequenceGenerator.setCacheSize(1000);
		sequenceGenerator.setSequenceCache(sequenceCache);
		
		final BaiduZhidaoCrawlerWorker worker = new BaiduZhidaoCrawlerWorker(savePath, sleepTime, sequenceGenerator);
		final ExecutorService executor = Executors.newFixedThreadPool(threads);
		for(int i = 0; i < threads; ++i) {
			executor.execute(worker);
		}
		
		// CTRL + C to terminate
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("shutting down...");
				executor.shutdownNow();
				System.out.println("exit, bye!");
			}
		});
		
		// wait to terminate 
		executor.shutdown();
		try {
			if(!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES)) {
				System.out.println("shutting down...");
				executor.shutdownNow();
				System.out.println("exit, bye!");
			}
		} catch (InterruptedException e) {
			
		}
	}

	private static int getSystemPropertyValueAsInt(String propertyName, int defaultValue) {
		String value = System.getProperty(propertyName);
		return value == null ? defaultValue : Integer.parseInt(value);
	}
}
