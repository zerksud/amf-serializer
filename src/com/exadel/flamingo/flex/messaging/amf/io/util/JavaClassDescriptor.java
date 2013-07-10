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

package com.exadel.flamingo.flex.messaging.amf.io.util;


import com.exadel.flamingo.flex.messaging.amf.io.util.externalizer.Externalizer;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Franck WOLFF
 */
public abstract class JavaClassDescriptor {

    protected final Class<?> type;
    protected final String name;
    protected final Externalizer externalizer;
    protected final Converter converter;
    protected final byte encoding;
    protected final List<Property> properties;

    protected JavaClassDescriptor(Class<?> type) {
        this.type = type;
        this.name = getClassName(type);
        this.externalizer = null;
        this.converter = new DefaultConverter();
        this.encoding = findEncoding(type);
        this.properties = new ArrayList<Property>();

        this.properties.addAll(introspectProperties());
    }

    private byte findEncoding(Class<?> type) {
        if (externalizer != null || Externalizable.class.isAssignableFrom(type))
            return 0x01;
        if (Map.class.isAssignableFrom(type))
            return 0x02;
        return 0x00;
    }

    protected abstract List<Property> introspectProperties();

    public static String getClassName(Class<?> clazz) {

        if (Map.class.isAssignableFrom(clazz) &&
            !Externalizable.class.isAssignableFrom(clazz))
            return "";
        return clazz.getName();
    }

    public Class<?> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Externalizer getExternalizer() {
        return externalizer;
    }

    public byte getEncoding() {
        return encoding;
    }

    public boolean isExternalizable() {
        return encoding == 0x01;
    }

    public boolean isDynamic() {
        return encoding == 0x02;
    }

    public int getPropertiesCount() {
        return properties.size();
    }

    public String getPropertyName(int index) {
        return properties.get(index).getName();
    }
    
    public void removeProperty(int index) {
    	properties.remove(index);
    }
    
    public Object getPropertyValue(int index, Object instance) {
        Property prop = properties.get(index);
        return prop.getProperty(instance);
    }
}