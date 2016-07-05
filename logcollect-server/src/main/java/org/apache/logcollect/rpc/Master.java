package org.apache.logcollect.rpc;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.HadoopIllegalArgumentException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtocolSignature;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RPC.Server;
public class Master implements ClientProtocol{
	public static final Log LOG = LogFactory.getLog(Master.class);
	
	private Server server;
	public Master(Configuration conf) throws HadoopIllegalArgumentException, IOException{
		this.server = new RPC.Builder(conf).setProtocol(ClientProtocol.class)
                .setInstance(this).setBindAddress("10.58.50.67").setPort(8787)
                .setNumHandlers(10).build();
		this.server.start();
		LOG.info("start Master");
	}
	/**
	   * Wait for service to finish.
	   * (Normally, it runs forever.)
	   */
    public void join() {
    	try {
    		this.server.join();
    	} catch (InterruptedException ie) {
    	
    	}
    }
	public static Master createMaster(){
		Configuration conf = new Configuration();
		Master master = null;
		try {
			master = new Master(conf);
		} catch (HadoopIllegalArgumentException e) {
			LOG.error("error ", e);
		} catch (IOException e) {
			LOG.error("error ", e);
		}
		return master;
	}
	public static void main(String[] args)   throws IOException{
		Master master = createMaster();
		if(master != null){
			master.join();
		}
	}

	public long getProtocolVersion(String protocol, long clientVersion)
			throws IOException {
		return ClientProtocol.versionID;
	}

	public ProtocolSignature getProtocolSignature(String protocol,
			long clientVersion, int clientMethodsHash) throws IOException {
		return new ProtocolSignature(ClientProtocol.versionID, null);
	}

	public String sendHeartbeat(String value) throws IOException {
		LOG.info("receive  heart beat from host " + value);
		return "receive  heart beat from host " + value;
	}
}
