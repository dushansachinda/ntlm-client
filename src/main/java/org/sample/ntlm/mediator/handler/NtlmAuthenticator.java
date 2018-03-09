package org.sample.ntlm.mediator.handler;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import uncertain.composite.CompositeMap;

import jcifs.Config;
import jcifs.UniAddress;
import jcifs.http.NtlmSsp;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbSession;
import jcifs.util.Base64;

public class NtlmAuthenticator {
	private String defaultDomain;
	private String domainController;
	private String realm;
	private boolean offerBasic;
	
	public static void main (String ar []){
		NtlmAuthenticator authenticator = new NtlmAuthenticator();
		String request ="NTLM TlRMTVNTUAADAAAAGAAYAEgAAACYAJgAYAAAABIAEgD4AAAADAAMAAoBAAAGAAYAFgEAAAAAAAAcAQAABYKIogUBKAoAAAAPTIaY9VSNp3BvJVgUxQaK3DW0nioVVGSqodG+GzQCX/7uvBOxjq5oZgEBAAAAAAAAcAdxDrCu0AFlCXEGw//90gAAAAACABIARABVAFMASABBAE4ALQBQAEMAAQASAEQAVQBTAEgAQQBOAC0AUABDAAQAEgBkAHUAcwBoAGEAbgAtAHAAYwADABIAZAB1AHMAaABhAG4ALQBwAGMABwAIAJTacA6wrtABAAAAAAAAAABEAFUAUwBIAEEATgAtAFAAQwBkAHUAcwBoAGEAbgAxADkAMgA=";
		try {
			authenticator.authenticate(request);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public NtlmPasswordAuthentication authenticate(String autherizationHeder) throws IOException, ServletException {
		UniAddress dc;
		String msg;
		NtlmPasswordAuthentication ntlm = null;
		msg = autherizationHeder;
		if (msg != null && msg.startsWith("NTLM ")) {
			byte[] token = Base64.decode(msg.substring(5));
			Type1Message type1=null;
			Type3Message type3=null;
			if (token[8] == 1) {
				type1 = new Type1Message(token);
				defaultDomain = type1.getSuppliedDomain();				
			} else if (token[8] == 3) {
				type3 = new Type3Message(token);
				defaultDomain = type3.getDomain();
				String paswrod= type3.getDefaultPassword();
				System.out.println(paswrod);
				System.out.println(type3.getUser());
				System.out.println(type3.getDomain());
			}
			return ntlm;
		}
		return null;		
	}	
}