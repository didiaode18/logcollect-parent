package org.apache.logcollect.rpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class Worker implements Runnable,NodeInfo{
	volatile boolean shouldRun = true;
	private Thread workerThread = null;
	ClientProtocol namenode;
	long lastHeartbeat = 0;
	static long heartBeatInterval = 3;
	public static final Log LOG = LogFactory.getLog(Worker.class);
	private static ScheduledExecutorService executorService;
	public static void main(String[] args) throws IOException{
		startWorker();
	}
	public static Worker createWorker(){
		Configuration conf = new Configuration();
		Worker wc = null;
		try {
			wc = new Worker(conf);
			runDatanodeDaemon(wc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return wc;
	}
	public static void runDatanodeDaemon(Worker wc) throws IOException {
		executorService = Executors.newSingleThreadScheduledExecutor(
	            new ThreadFactoryBuilder().setNameFormat("conf-file-poller-%d")
	                .build());
		executorService.scheduleWithFixedDelay(wc, 0, heartBeatInterval,
		        TimeUnit.SECONDS);
	  }
	public Worker(Configuration conf) throws IOException{
		InetSocketAddress nameNodeAddr = new InetSocketAddress("10.58.50.67",8787);
		namenode = RPC.waitForProxy(ClientProtocol.class,
				ClientProtocol.versionID,
                nameNodeAddr, 
                conf);
	}
	@SuppressWarnings("null")
	public static void startWorker(){
		Worker wc = createWorker();
		if(wc == null)
			wc.join();
	}
	void join() {
	    if (workerThread != null) {
	      try {
	    	  workerThread.join();
	      } catch (InterruptedException e) {}
	    }
	  }
	static long now() {
	    return System.currentTimeMillis();
	}
	public void run() {
		try {
			long startTime = now();
			if (startTime - lastHeartbeat > heartBeatInterval) {
				lastHeartbeat = startTime;
				LOG.info(namenode.sendHeartbeat(getHostName()));
			}
		} catch (IOException e) {
			LOG.warn("DataNode is shutting down: ");
			shutdown();
			return;
		}
	}
	public void shutdown() {
		this.shouldRun = false;
		RPC.stopProxy(namenode); // stop the RPC threads
		if (workerThread != null) {
			workerThread.interrupt();
		    try {
		    	workerThread.join();
		    } catch (InterruptedException ie) {
		    }
		}
	}
	public String getHostName() {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			String ip=addr.getHostAddress().toString();
			if(ip != null){
				return ip;
			}
			String address=addr.getHostName().toString();
			return address;
		} catch (UnknownHostException e) {
			LOG.error("error", e);
		}
		
		return null;
	}
}
