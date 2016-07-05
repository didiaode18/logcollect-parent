package org.apache.logcollect.node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.flume.Channel;
import org.apache.flume.Constants;
import org.apache.flume.SinkRunner;
import org.apache.flume.SourceRunner;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.lifecycle.LifecycleSupervisor;
import org.apache.flume.lifecycle.LifecycleSupervisor.SupervisorPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class NodeServer extends Thread{
	
	private String[] args;
	
	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	@Override
	public void run() {
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
		}catch (Exception e) {
			logger.error("A fatal error occurred while running. Exception follows.",e);
		}
	}
	private static final Logger logger = LoggerFactory
		      .getLogger(NodeServer.class);

	public static final String CONF_MONITOR_CLASS = "flume.monitoring.type";
	public static final String CONF_MONITOR_PREFIX = "flume.monitoring.";
	private MaterializedConfiguration materializedConfiguration;
	private final List<LifecycleAware> components;
	private final LifecycleSupervisor supervisor;
	
	public MaterializedConfiguration getMaterializedConfiguration() {
		return materializedConfiguration;
	}

	public NodeServer() {
	    this(new ArrayList<LifecycleAware>(0));
	}

	public NodeServer(List<LifecycleAware> components) {
		this.components = components;
		supervisor = new LifecycleSupervisor();
	}
	public synchronized void startComponent() {
		for(LifecycleAware component : components) {
			supervisor.supervise(component,
	          new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
	    }
	}
	@Subscribe
	public synchronized void handleConfigurationEvent(MaterializedConfiguration conf) {
		logger.info("========handleConfigurationEvent============");
	    stopAllComponents();
	    startAllComponents(conf);
	}
	
	public void stopAllComponents() {
		
	    if (this.materializedConfiguration != null) {
	      logger.info("Shutting down configuration: {}", this.materializedConfiguration);
	      for (Entry<String, SourceRunner> entry : this.materializedConfiguration
	          .getSourceRunners().entrySet()) {
	        try{
	          logger.info("Stopping Source " + entry.getKey());
	          supervisor.unsupervise(entry.getValue());
	        } catch (Exception e){
	          logger.error("Error while stopping {}", entry.getValue(), e);
	        }
	      }

	      for (Entry<String, SinkRunner> entry :
	        this.materializedConfiguration.getSinkRunners().entrySet()) {
	        try{
	          logger.info("Stopping Sink " + entry.getKey());
	          supervisor.unsupervise(entry.getValue());
	        } catch (Exception e){
	          logger.error("Error while stopping {}", entry.getValue(), e);
	        }
	      }

	      for (Entry<String, Channel> entry :
	        this.materializedConfiguration.getChannels().entrySet()) {
	        try{
	          logger.info("Stopping Channel " + entry.getKey());
	          supervisor.unsupervise(entry.getValue());
	        } catch (Exception e){
	          logger.error("Error while stopping {}", entry.getValue(), e);
	        }
	      }
	    }else{
	    	logger.info("this.materializedConfiguration null");
	    }
/*	    if(monitorServer != null) {
	      monitorServer.stop();
	    }*/
	  }
	private void startAllComponents(MaterializedConfiguration materializedConfiguration) {
	    logger.info("Starting new configuration:{}", materializedConfiguration);

	    this.materializedConfiguration = materializedConfiguration;

	    for (Entry<String, Channel> entry :
	      materializedConfiguration.getChannels().entrySet()) {
	      try{
	        logger.info("Starting Channel " + entry.getKey());
	        supervisor.supervise(entry.getValue(),
	            new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
	      } catch (Exception e){
	        logger.error("Error while starting {}", entry.getValue(), e);
	      }
	    }

	    /*
	     * Wait for all channels to start.
	     */
	    for(Channel ch: materializedConfiguration.getChannels().values()){
	      while(ch.getLifecycleState() != LifecycleState.START
	          && !supervisor.isComponentInErrorState(ch)){
	        try {
	          logger.info("Waiting for channel: " + ch.getName() +
	              " to start. Sleeping for 500 ms");
	          Thread.sleep(500);
	        } catch (InterruptedException e) {
	          logger.error("Interrupted while waiting for channel to start.", e);
	          Throwables.propagate(e);
	        }
	      }
	    }

	    for (Entry<String, SinkRunner> entry : materializedConfiguration.getSinkRunners()
	        .entrySet()) {
	      try{
	        logger.info("Starting Sink " + entry.getKey());
	        supervisor.supervise(entry.getValue(),
	          new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
	      } catch (Exception e) {
	        logger.error("Error while starting {}", entry.getValue(), e);
	      }
	    }

	    for (Entry<String, SourceRunner> entry : materializedConfiguration
	        .getSourceRunners().entrySet()) {
	      try{
	        logger.info("Starting Source " + entry.getKey());
	        supervisor.supervise(entry.getValue(),
	          new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
	      } catch (Exception e) {
	        logger.error("Error while starting {}", entry.getValue(), e);
	      }
	    }

	    //this.loadMonitoring();
	  }
	public static void main(String[] args) {
		
	}

}
