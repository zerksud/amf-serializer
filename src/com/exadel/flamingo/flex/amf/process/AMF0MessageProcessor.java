/*
  Exadel Flamingo
  Copyright (C) 2008 Exadel, Inc.

  Exadel Flamingo is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation

  AMF0MessageProcessor.java
  Last modified by: $Author$
  $Revision$   $Date$
 */

package com.exadel.flamingo.flex.amf.process;

import com.exadel.flamingo.flex.amf.AMF0Body;
import com.exadel.flamingo.flex.amf.AMF0Message;
import com.exadel.flamingo.flex.amf.AMF3Object;
import com.exadel.flamingo.flex.messaging.util.UUIDUtil;
import flex.messaging.messages.ErrorMessage;
import flex.messaging.messages.Message;

import java.util.Iterator;
import java.util.List;

/**
 * Process AMF0Message.
 * 
 * @author apleskatsevich
 */
public class AMF0MessageProcessor {

    private IAMF3MessageProcessor amf3MessageProcessor;

    public AMF0MessageProcessor(IAMF3MessageProcessor amf3MessageProcessor) {
        this.amf3MessageProcessor = amf3MessageProcessor;
    }

    public AMF0Message process(AMF0Message amf0RequestMessage) {
        return process(amf0RequestMessage, false, null);
    }

    public AMF0Message process(AMF0Message amf0RequestMessage, boolean needSessionTimeoutException,
            Throwable sessionException) {
        AMF0Message amf0ResponseMessage = new AMF0Message();
        amf0ResponseMessage.setVersion(amf0RequestMessage.getVersion());

        ErrorMessage loginError = null;
        String dsId = null;
        for (Iterator<AMF0Body> bodies = amf0RequestMessage.getBodies(); bodies.hasNext();) {
            AMF0Body requestBody = bodies.next();

            Message amf3RequestMessage = (Message) ((List<?>) requestBody.getValue()).get(0);

            Message amf3ResponseMessage = null;

            if (needSessionTimeoutException) {
                ErrorMessage sessionExceptionMessage = new ErrorMessage(sessionException);
                amf3ResponseMessage = sessionExceptionMessage.copy(amf3RequestMessage);
            } else {

                if (loginError == null) {
                    amf3ResponseMessage = amf3MessageProcessor.process(amf3RequestMessage);

                    if ((amf3ResponseMessage instanceof ErrorMessage)
                            && ((ErrorMessage) amf3ResponseMessage).loginError()) {
                        loginError = (ErrorMessage) amf3ResponseMessage;
                    }

                    // For SDK 2.0.1_Hotfix2+ (LCDS 2.5+).
                    if ("nil".equals(amf3ResponseMessage.getHeader(Message.DS_ID_HEADER))) {
                        amf3ResponseMessage.getHeaders().put(Message.DS_ID_HEADER,
                                (dsId == null ? (dsId = UUIDUtil.randomUUID()) : dsId));
                    }
                } else {
                    amf3ResponseMessage = loginError.copy(amf3RequestMessage);
                }

            }
            AMF3Object data = new AMF3Object(amf3ResponseMessage);
            AMF0Body responseBody = new AMF0Body(getResponseTarget(requestBody, amf3ResponseMessage), "", data,
                    AMF0Body.DATA_TYPE_AMF3_OBJECT);
            amf0ResponseMessage.addBody(responseBody);
        }

        return amf0ResponseMessage;
    }

    private static String getResponseTarget(AMF0Body requestBody, Message responseMessage) {
        if (responseMessage instanceof ErrorMessage) {
            return requestBody.getResponse() + "/onStatus";
        }
        return requestBody.getResponse() + "/onResult";
    }
}
