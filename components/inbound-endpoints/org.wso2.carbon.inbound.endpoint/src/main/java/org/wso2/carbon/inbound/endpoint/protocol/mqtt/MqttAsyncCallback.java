/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.inbound.endpoint.protocol.mqtt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
/**
 * MQTT Synchronous call back handler
 */
public class MqttAsyncCallback implements MqttCallback {

    private static final Log log = LogFactory.getLog(MqttAsyncCallback.class);

    private MqttInjectHandler injectHandler;
    private MqttConnectionFactory confac;
    private MqttAsyncClient mqttAsyncClient;
    private Properties mqttProperties;
    private MqttConnectOptions connectOptions;
    private MqttConnectionConsumer connectionConsumer;

    public MqttAsyncCallback(MqttAsyncClient mqttAsyncClient, MqttInjectHandler injectHandler,
                             MqttConnectionFactory confac, MqttConnectOptions connectOptions,
                             Properties mqttProperties) {
        this.injectHandler = injectHandler;
        this.mqttAsyncClient = mqttAsyncClient;
        this.confac = confac;
        this.connectOptions = connectOptions;
        this.mqttProperties = mqttProperties;

    }

    /**
     * Handle losing connection with the server. Here we just print it to the test console.
     *
     * @param throwable Throwable connection lost
     */
    @Override
    public void connectionLost(Throwable throwable) {
        log.info("Connection lost occurred to the remote server.");
        reConnect();
    }

    private void reConnect() {
        if (mqttAsyncClient != null) {
            try {
                MqttConnectionListener connectionListener =
                        new MqttConnectionListener(connectionConsumer);
                mqttAsyncClient.connect(connectOptions,connectionListener);

                connectionConsumer.getTaskSuspensionSemaphore().acquire();

                int qosLevel = Integer.parseInt(mqttProperties
                        .getProperty(MqttConstants.MQTT_QOS));
                if (confac.getTopic() != null) {
                    mqttAsyncClient.subscribe(confac.getTopic(), qosLevel);
                }

                log.info("Re-Connected to the remote server.");
            } catch (MqttException ex) {
                log.error("Error while trying to subscribe to the remote ");
            } catch (InterruptedException ex) {
                log.error("Error while trying to subscribe to the remote ");
            }
        }
    }

    public void messageArrived(String topic, MqttMessage mqttMessage) throws MqttException {
        if (log.isDebugEnabled()) {
            log.debug("Received Message: Topic:" + topic + "  Message: " + mqttMessage);
        }
        log.info("Received Message: Topic: " + topic);
        injectHandler.invoke(mqttMessage);
    }

    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        log.info("message delivered .. : " + iMqttDeliveryToken.toString());
    }

    public void setMqttConnectionConsumer(MqttConnectionConsumer connectionConsumer){
        this.connectionConsumer = connectionConsumer;
    }
}
