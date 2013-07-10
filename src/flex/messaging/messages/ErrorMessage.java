/*
  GRANITE DATA SERVICES
  Copyright (C) 2007 ADEQUATE SYSTEMS SARL

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 3 of the License, or (at your
  option) any later version.
 
  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
  for more details.
 
  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package flex.messaging.messages;

import com.exadel.flamingo.flex.messaging.security.SecurityServiceException;
import com.exadel.flamingo.flex.messaging.security.ServiceException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * @author Franck WOLFF
 */
public class ErrorMessage extends AcknowledgeMessage {

    private static final long serialVersionUID = 1L;

    private String faultCode = "Server.Call.Failed";
    private String faultDetail;
    private String faultString;
    private Object rootCause;
    private Map<String, Object> extendedData;

    private transient boolean loginError = false;
    
    public ErrorMessage() {
        super();
    }
    
    public ErrorMessage(Throwable t) {
        super();
        init(t);
    }
    
    public ErrorMessage(Message request, Throwable t) {
        super(request);
        if (request instanceof CommandMessage) {
            loginError = (
                ((CommandMessage)request).isLoginOperation() &&
                (t instanceof SecurityServiceException)
            );
        }
        init(t);
    }

    private void init(Throwable t) {
        if (t instanceof ServiceException) {
            ServiceException se = (ServiceException)t;
            
            this.faultCode = se.getCode();
            this.faultString = se.getMessage();
            
            if (t instanceof SecurityServiceException)
                this.faultDetail = se.getDetail();
            else {
                this.faultDetail = se.getDetail() + getStackTrace(t);
                this.extendedData = se.getExtendedData();
            }
        }
        else if (t != null) {
            this.faultString = t.getMessage();
            this.faultDetail = getStackTrace(t);
        }
        
        if (!(t instanceof SecurityServiceException)) {
            for (Throwable root = t; root != null; root = root.getCause())
                rootCause = root;
        }
    }
    
    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString().replace("\r\n", "\n").replace('\r', '\n');
    }

    public String getFaultCode() {
        return faultCode;
    }
    public void setFaultCode(String faultCode) {
        this.faultCode = faultCode;
    }

    public String getFaultDetail() {
        return faultDetail;
    }
    public void setFaultDetail(String faultDetail) {
        this.faultDetail = faultDetail;
    }

    public String getFaultString() {
        return faultString;
    }
    public void setFaultString(String faultString) {
        this.faultString = faultString;
    }

    public Map<String, Object> getExtendedData() {
        return extendedData;
    }
    public void setExtendedData(Map<String, Object> extendedData) {
        this.extendedData = extendedData;
    }

    public Object getRootCause() {
        return rootCause;
    }
    public void setRootCause(Object rootCause) {
        this.rootCause = rootCause;
    }
    
    public boolean loginError() {
        return loginError;
    }

    public ErrorMessage copy(Message request) {
        // Do not copy rootCause and extendedData
        ErrorMessage copy = new ErrorMessage(request, null);
        copy.faultCode = faultCode;
        copy.faultDetail = faultDetail;
        copy.faultString = faultString;
        copy.loginError = loginError;
        return copy;
    }
    
    @Override
    public String toString() {
    	return toString("");
    }
    
    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(getClass().getName()).append(" {");
        sb.append('\n').append(indent).append("  faultCode = ").append(faultCode);
        sb.append('\n').append(indent).append("  faultDetail = ").append(faultDetail);
        sb.append('\n').append(indent).append("  faultString = ").append(faultString);
        sb.append('\n').append(indent).append("  rootCause = ").append(rootCause);
        sb.append('\n').append(indent).append("  extendedData = ").append(extendedData);
        super.toString(sb, indent, null);
        sb.append('\n').append(indent).append('}');
        return sb.toString();
    }
}
