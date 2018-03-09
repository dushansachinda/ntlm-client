/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.sample.ntlm.mediator.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.httpclient.auth.AuthPolicy;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class WSClient {

    // update the following constants accordingly
    public static final String ACTION = "http://tempuri.org/IService1/GetData";
    public static final String EPR = "http://192.168.56.101/WcfService1/Service1.svc";
    public static final String USERNAME = "dushan";
    public static final String PASSWORD = "hm";
    public static final String HOST = "192.168.56.101";
    public static final String DOMAIN = "Dushan-PC11ee";

    public static void main(String[] args) {
        try {
        	
        	System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

        	System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");

        	System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");

        	System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

            ConfigurationContext ctx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    "/Users/dushan/workspace/onlinesupport/ESB/NLTM/ntlm-client/axis2_blocking_client.xml");
            ServiceClient serviceClient = new ServiceClient(ctx, null);
            Options options = serviceClient.getOptions();

            options.setAction(ACTION);
            options.setTo(new EndpointReference(EPR));

            jcifs.Config.setProperty("jcifs.encoding", "ASCII");
            AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, CustomNTLMScheme.class);

            HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
            List<String> authScheme = new ArrayList<String>();
            authScheme.add(HttpTransportProperties.Authenticator.NTLM);
            authenticator.setAuthSchemes(authScheme);
            authenticator.setUsername(USERNAME);
            authenticator.setPassword(PASSWORD);
            authenticator.setHost(HOST);
            authenticator.setDomain(DOMAIN);
            options.setProperty(HTTPConstants.AUTHENTICATE, authenticator);
            options.setProperty(HTTPConstants.HEADER_CONNECTION_KEEPALIVE, Boolean.FALSE);

            OMElement response = serviceClient.sendReceive(createPayload());
            System.out.println(response);

        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }
    }

    /**
     * This method creates the payload
     * @return OMElement with the payload
     */
    private static OMElement createPayload() {
        try {
            String payloadStr =
                    "      <tem:GetData xmlns:tem=\"http://tempuri.org/\">\n" +
                            "         <!--Optional:-->\n" +
                            "         <tem:value>2</tem:value>\n" +
                            "      </tem:GetData>";
            return AXIOMUtil.stringToOM(payloadStr);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return null;
    }
}
