package org.apache.logcollect.server;

import org.apache.logcollect.rpc.Worker;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogAgent {
	private static final Logger logger = LoggerFactory
		      .getLogger(LogAgent.class);
	public static void main(String[] args) throws Exception {
		logger.info("start LogAgent");
		Server server = new Server();
	    Connector connector = new SocketConnector();
	    connector.setPort(8090);
	    Worker.startWorker();
	    server.setConnectors(new Connector[] { connector });
	    Handler handler = new ServerHandler();
	    server.setHandler(handler);
	    server.setStopAtShutdown(true);
	    server.setSendServerVersion(true);
	    server.start();
	    server.join();
	}

}
