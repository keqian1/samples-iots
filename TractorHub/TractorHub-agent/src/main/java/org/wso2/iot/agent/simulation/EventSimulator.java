/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.iot.agent.simulation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.iot.agent.analytics.SiddhiEngine;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;

import javax.swing.*;

public class EventSimulator {

    private static final Log log = LogFactory.getLog(EventSimulator.class);
    private final SiddhiEngine siddhiEngine;
    private AgentUI agentUI;

    public EventSimulator(SiddhiEngine siddhiEngine) {
        this.siddhiEngine = siddhiEngine;

        try {
            // Set System L&F for Device UI
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
            log.error(
                    "'UnsupportedLookAndFeelException' error occurred whilst initializing the" +
                    " Agent UI.");
        } catch (ClassNotFoundException e) {
            log.error(
                    "'ClassNotFoundException' error occurred whilst initializing the Agent UI.");
        } catch (InstantiationException e) {
            log.error(
                    "'InstantiationException' error occurred whilst initializing the Agent UI.");
        } catch (IllegalAccessException e) {
            log.error(
                    "'IllegalAccessException' error occurred whilst initializing the Agent UI.");
        }
    }

    public void start() {
        java.awt.EventQueue.invokeLater(() -> {
            agentUI = new AgentUI(siddhiEngine);
            agentUI.setVisible(true);
            siddhiEngine.addQueryCallback("alert_query", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    log.info("Event received to alert query");
                    EventPrinter.print(timestamp, inEvents, removeEvents);
                    if (inEvents == null) {
                        return;
                    }
                    for (Event event : inEvents) {
                        agentUI.addAlert(event.getData(0).toString());
                    }
                }
            });
            publishData();
        });
    }

    private void publishData() {
        Runnable simulator = () -> {
            boolean interrupted = false;
            InputHandler inputHandler;
            while (!interrupted) {
                try {
                    inputHandler = siddhiEngine.getInputHandler("agentEventStream");
                    inputHandler.send(new Object[]{agentUI.getEngineTemp(), agentUI.getHumidity(),
                                                   agentUI.getTractorSpeed(), agentUI.getLoadWeight(),
                                                   agentUI.getSoilMoisture(), agentUI.getIllumination(),
                                                   agentUI.getFuelUsage(), agentUI.isEngineIdle(),
                                                   agentUI.isRaining(), agentUI.getTemperature()});
                    log.info("New event emitted.");
                    Thread.sleep(agentUI.getInterval());
                } catch (InterruptedException e) {
                    log.warn("Thread interrupted.", e);
                    interrupted = true;
                    Thread.currentThread().interrupt();
                }
            }
        };
        new Thread(simulator).start();
    }
}
