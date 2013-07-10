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

package flex.messaging.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Franck WOLFF
 */
public class ArrayCollection extends ArrayList<Object> implements Externalizable {

    private static final long serialVersionUID = 1L;

    public ArrayCollection() {
        super();
    }

    public ArrayCollection(int capacity) {
        super(capacity);
    }
    
    public ArrayCollection(Collection<?> col) {
        super(col);
    }
    
    public ArrayCollection(Object[] array) {
        super();
        addAll(array);
    }

    public void addAll(Object[] array) {
        for (Object o : array)
            add(o);
    }
    
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(toArray());
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        Object o = in.readObject();
        if (o != null) {
            if (o instanceof Collection)
                addAll((Collection<?>)o);
            else if (o.getClass().isArray())
                addAll((Object[])o);
            else // should we throw an exception ?
                add(o);
        }
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
}
