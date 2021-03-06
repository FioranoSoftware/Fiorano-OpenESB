/*
 * Copyright (c) Fiorano Software and affiliates. All rights reserved. http://www.fiorano.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.fiorano.openesb.microservice.launch.impl;

import com.fiorano.openesb.application.DmiObject;
import com.fiorano.openesb.application.application.LogManager;
import com.fiorano.openesb.application.service.LogModule;
import com.fiorano.openesb.application.service.RuntimeArgument;
import com.fiorano.openesb.application.service.Service;
import com.fiorano.openesb.application.service.ServiceParser;
import com.fiorano.openesb.microservice.launch.AdditionalConfiguration;
import com.fiorano.openesb.microservice.launch.LaunchConfiguration;
import com.fiorano.openesb.microservice.launch.LaunchConstants;
import com.fiorano.openesb.utils.LookUpUtil;
import com.fiorano.openesb.utils.exception.FioranoException;

import java.io.File;
import java.util.*;

public abstract class CommandProvider<J extends AdditionalConfiguration> {

    protected abstract List<String> generateCommand(LaunchConfiguration<J> launchConfiguration) throws  Exception;

    protected List<String> getCommandLineParams(LaunchConfiguration<J> launchConfiguration) {
        Map<String, String> commandLineArgs = new LinkedHashMap<>();
        String connectURL = launchConfiguration.getAdditionalConfiguration().getProviderUrl();
        commandLineArgs.put(LaunchConstants.URL, connectURL);
        commandLineArgs.put(LaunchConstants.BACKUP_URL, connectURL);
        commandLineArgs.put(LaunchConstants.FES_URL, connectURL);
        String icf = launchConfiguration.getAdditionalConfiguration().getICF();
        if(icf != null) commandLineArgs.put(LaunchConstants.ICF, icf);
        commandLineArgs.put(LaunchConstants.USERNAME, launchConfiguration.getUserName());
        commandLineArgs.put(LaunchConstants.PASSWORD, launchConfiguration.getPassword());
        commandLineArgs.put(LaunchConstants.CONN_FACTORY,"ConnectionFactory");
        commandLineArgs.put(LaunchConstants.CLIENT_ID, LookUpUtil.getServiceInstanceLookupName(launchConfiguration.getApplicationName(),
                launchConfiguration.getApplicationVersion(), launchConfiguration.getServiceName()));
        commandLineArgs.put(LaunchConstants.EVENT_PROC_NAME, launchConfiguration.getApplicationName());
        commandLineArgs.put(LaunchConstants.EVENT_PROC_VERSION, launchConfiguration.getApplicationVersion());
        commandLineArgs.put(LaunchConstants.COMP_INSTANCE_NAME, launchConfiguration.getServiceName());

        commandLineArgs.put(LaunchConstants.IS_IN_MEMORY, launchConfiguration.getLaunchMode() == LaunchConfiguration.
                LaunchMode.IN_MEMORY ? "true" : "false");
        commandLineArgs.put(LaunchConstants.CCP_ENABLED, "true");
        commandLineArgs.put(LaunchConstants.COMPONENT_REPO_PATH, launchConfiguration.getAdditionalConfiguration().getCompRepoPath());
        commandLineArgs.put(LaunchConstants.COMPONENT_GUID, launchConfiguration.getMicroserviceId());
        commandLineArgs.put(LaunchConstants.COMPONENT_VERSION, launchConfiguration.getMicroserviceVersion());
        commandLineArgs.put(LaunchConstants.JETTY_URL, launchConfiguration.getAdditionalConfiguration().getJettyUrl());
        commandLineArgs.put(LaunchConstants.JETTY_URL_SSL, launchConfiguration.getAdditionalConfiguration().getJettySSLUrl());
        List logmodules = launchConfiguration.getLogModules();
        StringBuilder sb = new StringBuilder();
        for(Object object:logmodules){
            LogModule logModule = (LogModule) object;
            sb.append(logModule.getName());
            sb.append("=");
            sb.append(logModule.getTraceLevelAsString());
            sb.append(",");
        }
        commandLineArgs.put(LaunchConstants.LOG_HANDLERS,  sb.toString());
        LogManager logManager = launchConfiguration.getLogManager();
        sb = new StringBuilder();
        sb.append("loggerClass=");
        sb.append(logManager.getLoggerClass());
        sb.append(",");
        for (Map.Entry<Object, Object> objectObjectEntry : logManager.getProps().entrySet()) {
            sb.append((String) objectObjectEntry.getKey());
            sb.append("=");
            sb.append((String) objectObjectEntry.getValue());
            sb.append(",");
        }
        commandLineArgs.put(LaunchConstants.LOG_MANAGER, sb.toString());
        commandLineArgs.put("java.util.logging.FileHandler.dir", "log");
        commandLineArgs.put(LaunchConstants.PRODUCT_NAME, "openESB");

        RuntimeArgument arg = (RuntimeArgument) DmiObject.findNamedObject(launchConfiguration.getRuntimeArgs(), LaunchConstants.JCA_INTERACTION_SPEC);
        if (arg != null)
            commandLineArgs.put(LaunchConstants.JCA_INTERACTION_SPEC, arg.getValueAsString());

        for (RuntimeArgument runtimeArg : launchConfiguration.getRuntimeArgs()) {
            String argValue = runtimeArg.getValueAsString();
            if (!runtimeArg.getName().equalsIgnoreCase("JVM_PARAMS") && argValue!=null)
                commandLineArgs.put(runtimeArg.getName(), runtimeArg.getValueAsString());
        }

        List<String> commandLineParams = new ArrayList<>();
        for(Map.Entry<String, String> entry:commandLineArgs.entrySet()){
            commandLineParams.add(entry.getKey());
            commandLineParams.add(entry.getValue());
        }
        return commandLineParams;
    }

    protected String getExecutionDir(LaunchConfiguration launchConfiguration) {
        return launchConfiguration.getAdditionalConfiguration().getCompRepoPath()+ File.separator +launchConfiguration.getMicroserviceId() +File.separator+
                launchConfiguration.getMicroserviceVersion();
    }

    protected Service getComponentPS(String compRepoPath, String componentGUID, String componentVersion) throws FioranoException {
        File sdFile = new File(compRepoPath+ File.separator +componentGUID +File.separator+componentVersion  + File.separator + "ServiceDescriptor.xml");
        return ServiceParser.readService(sdFile);
    }
}
