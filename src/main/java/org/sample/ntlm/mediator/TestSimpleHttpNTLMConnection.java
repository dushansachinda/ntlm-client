package org.sample.ntlm.mediator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ArrayList;
 
 
 
 













import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
//import org.junit.Test;
import org.sample.ntlm.mediator.connection.ConnectionManager;
import org.sample.ntlm.mediator.connection.CustomSSLSocketFactory;
 
public class TestSimpleHttpNTLMConnection {
 
	//@Test
	
	public static void main(String [] ar) throws ClientProtocolException, IOException{
		TestSimpleHttpNTLMConnection connection = new TestSimpleHttpNTLMConnection();
		connection.testConnection();
	}
	public void testConnection() throws ClientProtocolException, IOException {
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

    	System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");

    	System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");

    	System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
    	
		ConnectionManager  connectionMgr =null;
		try {
			  connectionMgr = ConnectionManager.getInstance(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CustomSSLSocketFactory customSSLSocketFactory = connectionMgr.getCustomSSLSocketFactory();
		DefaultHttpClient httpclient = (DefaultHttpClient) customSSLSocketFactory.getNewHttpClient("https",443,connectionMgr.getCustomSSLSocketFactory());//new DefaultHttpClient();
		//httpclient.setKeepAliveStrategy(); 
		List<String> authpref = new ArrayList<String>();
		authpref.add(AuthPolicy.NTLM);
		httpclient.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);
		NTCredentials creds = new NTCredentials("dushan", "hm", "192.168.56.101", "192.168.56.101");
		httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
 
		HttpHost target = new HttpHost("192.168.56.101", 443, "https");
 
		// Make sure the same context is used to execute logically related requests
		HttpContext localContext = new BasicHttpContext();
 
		// Execute a cheap method first. This will trigger NTLM authentication
		HttpGet httpget = new HttpGet("/RestService2/RestServiceImpl.svc/xml/123");
		//httpget.addHeader("Connection", "close");
		HttpResponse response = httpclient.execute(target, httpget, localContext);
		HttpEntity entity = response.getEntity();
		System.out.println(EntityUtils.toString(entity));
		
 
 
	}
	
	
	
 
}