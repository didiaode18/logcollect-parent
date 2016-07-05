package org.apache.logcollect.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.flume.Constants;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.logcollect.file.FileHelper;
import org.apache.logcollect.node.NodeServer;
import org.apache.logcollect.node.PollingPropertiesFileConfigurationProvider;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

public class ServerHandler extends AbstractHandler{
	
	private static String workspace = System.getProperty("user.dir");
	private static final Logger logger = LoggerFactory
		      .getLogger(ServerHandler.class);
	private static Map<String,Thread> threads = new HashMap<String,Thread>();
	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException {
		 	Request base_request = (Request) request;
	        response.setContentType("text/html");
	        response.setCharacterEncoding("UTF-8");
	        PrintWriter pw = response.getWriter();
	        if (target.equalsIgnoreCase("/AddHandler")) {
	        	/*String params = request.getParameter("params");
	        	ConfigStructs config = JSONObject.parseObject(params, ConfigStructs.class);
	        	System.out.println(config);
	        	String path = ServerHandler.workspace + "/../etc/" + config.getApplication();
	        	FileHelper.createFile(path, params);
	    	    pw.write("add success");*/
	        }else if (target.equalsIgnoreCase("/StartHandler")) {
	        	String params = request.getParameter("params");
	        	
	        	String path = ServerHandler.workspace + "/../etc/" + params;
	        	logger.info(path);
	        	String args = "-f " + path + " --name a1";
	        	try {
					init(params,args.split(" "));
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
	        	File file = new File(path);
	        	try{
	        		String conf = FileHelper.readFile(file);
	        		pw.write(conf);
	        	}catch(FileNotFoundException e){
	        		pw.write(e.getMessage());
	        	}
	        }else if (target.equalsIgnoreCase("/StopHandler")) {
	        	String params = request.getParameter("params");
	        	NodeServer ns = (NodeServer) threads.get(params);
	        	logger.info("StopHandler " + ns.getName());
	        	ns.stopAllComponents();
	        }
	        pw.flush();
	        base_request.setHandled(true);
	}
	
	public void init(String params,String args[]) throws ParseException{
		logger.info("=========== NodeClient Starting ===========");
		try{

		      Options options = new Options();

		      Option option = new Option("n", "name", true, "the name of this agent");
		      option.setRequired(true);
		      options.addOption(option);

		      option = new Option("f", "conf-file", true,
		          "specify a config file (required if -z missing)");
		      option.setRequired(false);
		      options.addOption(option);

		      option = new Option(null, "no-reload-conf", false,
		          "do not reload config file if changed");
		      options.addOption(option);

		      option = new Option("h", "help", false, "display help text");
		      options.addOption(option);

		      CommandLineParser parser = new GnuParser();
		      CommandLine commandLine = parser.parse(options, args);
		      if (commandLine.hasOption('h')) {
		        new HelpFormatter().printHelp("log-collect agent", options, true);
		        return;
		      }
		      
		      String agentName = commandLine.getOptionValue('n');
		      logger.info(agentName);
		      NodeServer server = null;
		      File configurationFile = new File(commandLine.getOptionValue('f'));
		      if (!configurationFile.exists()) {
		    	  if (System.getProperty(Constants.SYSPROP_CALLED_FROM_SERVICE) == null) {
		    		  String path = configurationFile.getPath();
		    		  logger.info(path);
		    		  try {
		                  path = configurationFile.getCanonicalPath();
		                  logger.info(path);
		              } catch (IOException ex) {
		                  logger.error("Failed to read canonical path for file: " + path,ex);
		              }
		    		  throw new ParseException("The specified configuration file does not exist: " + path);
		    	  }
		      }
		      List<LifecycleAware> components = Lists.newArrayList();
		      EventBus eventBus = new EventBus(agentName + "-event-bus");
		      PollingPropertiesFileConfigurationProvider configurationProvider =
		              new PollingPropertiesFileConfigurationProvider(
		                agentName, configurationFile, eventBus, 30);
		      components.add(configurationProvider);
		      server = new NodeServer(components);
	          eventBus.register(server);
	          server.startComponent();
	          logger.info("=========== NodeClient Started ===========");
	          threads.put(params, server);
		}catch (Exception e) {
			logger.error("A fatal error occurred while running. Exception follows.",e);
		}
	}
}
