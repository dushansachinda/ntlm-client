package org.sample.ntlm.mediator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
//import org.junit.Test;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.sample.ntlm.mediator.connection.ConnectionManager;

public class NTLMMediator extends AbstractMediator implements ManagedLifecycle {
	private static final String NTLM_PASSWORD = "NTLM_PASSWORD";
	private static final String NTLM_USERNAME = "NTLM_USERNAME";
	private static final Log log = LogFactory.getLog(NTLMMediator.class);
	private String host;
	private int port;
	private String domain;
	private String username;
	private String password;
	private final static String JSON_CONTENT_TYPE = "application/json";
	private final static String XML_CONTENT_TYPE = "application/xml";
	private final static String TEXT_XML_CONTENT_TYPE = "text/xml";
	private ConnectionManager connectionManager = null;

	// Enable wire logs in log4j.properties add following

	// log4j.logger.org.apache.http=DEBUG
	// log4j.logger.org.apache.http.wire=ERROR

	@Override
	public void init(SynapseEnvironment arg0) {
		// TODO Auto-generated method stub
		try {
			connectionManager = ConnectionManager.getInstance(true);
		} catch (Exception e) {
			handleException("Error while intializing connector", e, null);
		}
	}

	public String getPayload(MessageContext synMgtx, String contentType) {
		org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synMgtx).getAxis2MessageContext();
		if (contentType.equals(JSON_CONTENT_TYPE)) {
			// XML to JSON conversion here
			try {
				String replacementValue = "<jsonObject>" + a2mc.getEnvelope().getBody().getFirstElement().toString()
						+ "</jsonObject>";
				OMElement omXML = AXIOMUtil.stringToOM(replacementValue);
				replacementValue = JsonUtil.toJsonString(omXML).toString();
				return replacementValue;
			} catch (XMLStreamException e) {
				handleException(
						"Error parsing XML for JSON conversion, please check your xPath expressions return valid XML: ",
						synMgtx);
			} catch (AxisFault e) {
				handleException("Error converting XML to JSON", synMgtx);
			}
		} else {
			return a2mc.getEnvelope().toString();
		}

		return null;

	}

	public void mediateNTLM(MessageContext synMgtx) throws ClientProtocolException, IOException {
		org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synMgtx).getAxis2MessageContext();

		// httpclient.setKeepAliveStrategy();
		List<String> authpref = new ArrayList<String>();
		authpref.add(AuthPolicy.NTLM);
		URL aURL = new URL(synMgtx.getTo().getAddress());
		DefaultHttpClient httpclient = null;
		try {
			httpclient = (DefaultHttpClient) connectionManager.getHttpClient(synMgtx, port, aURL.getProtocol());
			httpclient.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);
			String ntlmusername = (String) synMgtx.getProperty(NTLM_USERNAME);
			String ntlmpassword = (String) synMgtx.getProperty(NTLM_PASSWORD);
			if (ntlmusername != null && !ntlmusername.isEmpty()) {
				username = ntlmusername;
			}
			if (ntlmpassword != null && !ntlmpassword.isEmpty()) {
				password = ntlmpassword;
			}

			NTCredentials creds = new NTCredentials(username, password, host, domain);
			httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);

			HttpHost target = new HttpHost(aURL.getHost(), port, aURL.getProtocol());

			// Make sure the same context is used to execute logically related
			// requests
			HttpContext localContext = new BasicHttpContext();

			// Execute a cheap method first. This will trigger NTLM
			// authentication
			HttpRequestBase httpMethod = handleMethod(synMgtx);

			if (synMgtx.getWSAAction() != null) {
				if (log.isDebugEnabled()) {
					log.debug("NTLM mediator ### SOAP message ##" + synMgtx.getWSAAction());
				}
				if (httpMethod.getHeaders("SOAPAction") == null) {
					httpMethod.addHeader("SOAPAction", (String) synMgtx.getSoapAction());
				}
			}

			HttpResponse response = httpclient.execute(target, httpMethod, localContext);
			HttpEntity entity = null;
			String responseEntity = "";
			try {
				entity = response.getEntity();
				responseEntity = EntityUtils.toString(entity);
			} catch (Exception e) {
				log.error("Error ", e);
				handleException("error while reading response", synMgtx);
			} finally {
				httpMethod.releaseConnection();
			}
			if (log.isDebugEnabled()) {
				log.debug("NTLM mediator ### RESPONSE received message ##" + entity.getContentType());
			}
			if (entity.getContentType().getValue().contains(JSON_CONTENT_TYPE)) {
				OMElement omXML = JsonUtil.toXml(IOUtils.toInputStream(responseEntity), false);
				if (omXML != null) {
					a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, JSON_CONTENT_TYPE);
					a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, JSON_CONTENT_TYPE);
					synMgtx.getEnvelope().getBody().addChild(omXML);
				} else {
					// synLog.traceOrDebug("Service returned a null response");
				}
			}

			if (entity.getContentType().getValue().contains(XML_CONTENT_TYPE)) {
				OMElement omXML = null;
				try {
					omXML = AXIOMUtil.stringToOM(responseEntity);
				} catch (XMLStreamException e) {
					log.error("Error ", e);
					handleException("error while building response", synMgtx);
				}
				if (omXML != null) {
					a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, XML_CONTENT_TYPE);
					a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, XML_CONTENT_TYPE);
					synMgtx.getEnvelope().getBody().addChild(omXML);
				} else {
					// synLog.traceOrDebug("Service returned a null response");
				}
			}
			// considering soap 1.1
			if (entity.getContentType().getValue().contains(TEXT_XML_CONTENT_TYPE) && synMgtx.getWSAAction() != null) {
				// if (contentType != null) {
				// loading builder from externally..
				// builder =
				// configuration.getMessageBuilder(_contentType,useFallbackBuilder);
				String _contentType = getContentType(entity.getContentType().getValue(), synMgtx);
				Builder builder = MessageProcessorSelector.getMessageBuilder(_contentType, a2mc);
				OMElement element = null;
				if (builder != null) {
					try {
						/*
						 * try { throw new Exception("Building message"); }
						 * catch (Exception e) { e.printStackTrace(); }
						 */
						element = builder.processDocument(IOUtils.toInputStream(responseEntity), _contentType, a2mc);
					} catch (AxisFault axisFault) {
						log.error("Error building message", axisFault);
						throw axisFault;
					}
				}
				synMgtx.setEnvelope((SOAPEnvelope) element);
			}

			a2mc.setProperty(SynapseConstants.HTTP_SC, response.getStatusLine().getStatusCode());
			Header[] _headers = response.getAllHeaders();
			Map<String, String> headers = new HashMap<String, String>();
			Map excessHeaders = new MultiValueMap();
			if (headers != null) {
				for (Header header : _headers) {
					excessHeaders.put(header.getName(), header.getValue());
				}
			}
			a2mc.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, excessHeaders);
		} catch (Exception e) {
			log.error("Error GENERIC mediation", e);
			handleException("Error", e, synMgtx);
		} finally {
			if (httpclient != null) {
				// httpclient.
			}
		}
		// TODO:remove invalid headers from response
		// TODO:handle soap 1.2
	}

	public static String getContentType(String contentType, MessageContext msgContext) {
		String type;
		int index = contentType.indexOf(';');
		if (index > 0) {
			type = contentType.substring(0, index);
		} else {
			int commaIndex = contentType.indexOf(',');
			if (commaIndex > 0) {
				type = contentType.substring(0, commaIndex);
			} else {
				type = contentType;
			}
		}
		// Some services send REST responses as text/xml. We should convert it
		// to
		// application/xml if its a REST response, if not it will try to use the
		// SOAPMessageBuilder.
		// isDoingREST should already be properly set by
		// HTTPTransportUtils.initializeMessageContext
		if (null != msgContext.getProperty(PassThroughConstants.INVOKED_REST)
				&& msgContext.getProperty(PassThroughConstants.INVOKED_REST).equals(true)
				&& HTTPConstants.MEDIA_TYPE_TEXT_XML.equals(type)) {
			type = HTTPConstants.MEDIA_TYPE_APPLICATION_XML;
		}
		return type;
	}

	private HttpRequestBase handleMethod(MessageContext synMgtx) throws UnsupportedEncodingException {
		org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synMgtx).getAxis2MessageContext();
		String httpMethod = (String) a2mc.getProperty(Constants.Configuration.HTTP_METHOD);
		if (Constants.Configuration.HTTP_METHOD_GET.equalsIgnoreCase(httpMethod)) {
			HttpGet httpget = new HttpGet(synMgtx.getTo().getAddress());
			return httpget;

		} else if (Constants.Configuration.HTTP_METHOD_POST.equalsIgnoreCase(httpMethod)) {
			HttpPost httpPost = new HttpPost(synMgtx.getTo().getAddress());
			if (JSON_CONTENT_TYPE.equals(a2mc.getProperty(Constants.Configuration.MESSAGE_TYPE))) {
				StringEntity se = new StringEntity(getPayload(synMgtx, JSON_CONTENT_TYPE), "UTF-8");
				se.setContentType(JSON_CONTENT_TYPE); // TODO:decided by
														// formatter
				httpPost.setEntity(se);
				addHeaders(httpPost, synMgtx);
			} else if (TEXT_XML_CONTENT_TYPE.equals(a2mc.getProperty(Constants.Configuration.MESSAGE_TYPE))) {
				StringEntity se = new StringEntity(getPayload(synMgtx, TEXT_XML_CONTENT_TYPE), "UTF-8");
				se.setContentType(TEXT_XML_CONTENT_TYPE); // TODO:decided by
															// formatter
				httpPost.setEntity(se);
				addHeaders(httpPost, synMgtx);
				if (synMgtx.getSoapAction() != null) {
					httpPost.addHeader("SOAPAction", (String) synMgtx.getSoapAction());
				}
			}

			return httpPost;
		}
		return null;
	}

	private void addHeaders(HttpPost httpPost, MessageContext synMgtx) {
		org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synMgtx).getAxis2MessageContext();

		for (Map.Entry<String, Object> entry : a2mc.getProperties().entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof String) {
				httpPost.addHeader(key, (String) value);
			}
		}

	}

	@Override
	public boolean mediate(MessageContext synMgtx) {
		try {
			this.mediateNTLM(synMgtx);
		} catch (ClientProtocolException e) {
			log.error("Error NTLM mediation", e);
			handleException("Error while connecting http client", e, synMgtx);
		} catch (IOException e) {
			log.error("Error NTLM mediation", e);
			handleException("Error while connecting http client", e, synMgtx);
		}
		return true;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}