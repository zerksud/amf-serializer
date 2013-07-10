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

import com.exadel.flamingo.flex.messaging.util.StringUtil;
import com.exadel.flamingo.flex.messaging.util.UUIDUtil;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Franck WOLFF
 */
public abstract class AbstractMessage implements Message {

    private Object body = null;
    private Object clientId = null;
    private String destination = null;
    private Map<String, Object> headers = null;
    private String messageId = null;
    private long timestamp = 0L;
    private long timeToLive = 0L;
    
    public AbstractMessage() {
        super();
    }
    
    public AbstractMessage(Message request) {
        super();
        this.messageId = UUIDUtil.randomUUID();
        this.timestamp = new Date().getTime();
        this.clientId = UUIDUtil.randomUUID();
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public Object getClientId() {
        return clientId;
    }

    public void setClientId(Object clientId) {
        this.clientId = clientId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Object getHeader(String name) {
        return (headers != null ? headers.get(name) : null);
    }

    public boolean headerExists(String name) {
        return (headers != null ? headers.containsKey(name) : false);
    }

    public void setHeader(String name, Object value) {
        if (headers == null)
            headers = new HashMap<String, Object>();
        
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    protected void toString(StringBuilder sb, String indent, String bodyAlternative) {
        sb.append('\n').append(indent).append("  destination = ").append(destination);
        
        if (headers != null && headers.containsKey(REMOTE_CREDENTIALS_HEADER)) {
            Map<String, Object> headersCopy = new HashMap<String, Object>(headers);
            headersCopy.put(REMOTE_CREDENTIALS_HEADER, HIDDEN_CREDENTIALS);
            sb.append('\n').append(indent).append("  headers = ").append(headersCopy);
        }
        else
            sb.append('\n').append(indent).append("  headers = ").append(headers);
        
        sb.append('\n').append(indent).append("  messageId = ").append(messageId);
        sb.append('\n').append(indent).append("  timestamp = ").append(timestamp);
        sb.append('\n').append(indent).append("  clientId = ").append(clientId);
        sb.append('\n').append(indent).append("  timeToLive = ").append(timeToLive);
        sb.append('\n').append(indent).append("  body = ").append(printBody(body, bodyAlternative));
    }
    
    private static String printBody(Object body, String bodyAlternative) {
        
    	body = (bodyAlternative != null ? bodyAlternative : body);
        if (body == null)
            return null;
    	
    	if (body.getClass().isArray() || body instanceof Collection || body instanceof Map)
    		return StringUtil.toString(body, 100); // limit to first 100 elements.

        return body.toString();
    }
}
