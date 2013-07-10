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

package com.exadel.flamingo.flex.messaging.amf.io.util.externalizer;

import com.exadel.flamingo.flex.messaging.amf.io.util.instanciator.EnumInstanciator;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Igor SAZHNEV
 */
public class EnumExternalizer extends DefaultExternalizer {

    @Override
    public void readExternal(Object o, ObjectInput in) throws IOException, ClassNotFoundException,
            IllegalAccessException {
        super.readExternal(o, in);
    }

    @Override
    public void writeExternal(Object o, ObjectOutput out) throws IOException,
            IllegalAccessException {
        out.writeObject(o.toString());
    }

    @Override
    public Object newInstance(String type, ObjectInput in) throws ClassNotFoundException,
            InstantiationException, InvocationTargetException, IllegalAccessException {
        return new EnumInstanciator(type);
    }
}
