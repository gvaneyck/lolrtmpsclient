package com.gvaneyck.rtmp;

import com.gvaneyck.rtmp.encoding.ObjectMap;

/**
 * A map that has a type, used to represent an object
 * 
 * @author Gabriel Van Eyck
 */
public class TypedObject extends ObjectMap
{
	private static final long serialVersionUID = 1244827787088018807L;

	public String type;
	
	/**
	 * Creates a typed object that is simply a map (null type)
	 */
	public TypedObject()
	{
		this.type = null;
	}

	/**
	 * Initializes the type of the object, null type implies a dynamic class
	 * (used for headers and some other things)
	 * 
	 * @param type The type of the object
	 */
	public TypedObject(String type)
	{
		this.type = type;
	}

	/**
	 * Creates a flex.messaging.io.ArrayCollection in the structure that the
	 * encoder expects
	 * 
	 * @param data The data for the ArrayCollection
	 * @return
	 */
	public static TypedObject makeArrayCollection(Object[] data)
	{
		TypedObject ret = new TypedObject("flex.messaging.io.ArrayCollection");
		ret.put("array", data);
		return ret;
	}

	/**
	 * Convenience for going through object hierarchy
	 * 
	 * @param key The key of the TypedObject
	 * @return The TypedObject
	 */
	public TypedObject getTO(String key)
	{
		return (TypedObject)get(key);
	}
	
	/**
  	 * Convenience for retrieving object arrays
  	 * Also handles flex.messaging.io.ArrayCollection
  	 *
  	 * @param key The key of the object array
  	 * @return The object array
  	 */
  	public Object[] getArray(String key)
  	{
  		if (get(key) instanceof TypedObject && getTO(key).type.equals("flex.messaging.io.ArrayCollection"))
  			return (Object[])getTO(key).get("array");
  		else
  			return (Object[])get(key);
  	}
  	
  	public String toString() {
  		StringBuilder buff = new StringBuilder();
  		buff.append("{");
  		for (String key : keySet()) {
  			buff.append(key);
  			buff.append('=');
  			if (key.equals("array")) {
  				buff.append('[');
  				for (Object o : getArray(key)) {
  					buff.append(o.toString());
  					buff.append(", ");
  				}
  				buff.append(']');
  			}
  			else if (get(key) instanceof Double) {
  				buff.append(((Double)get(key)).longValue());
  			}
  			else
  				buff.append(get(key));
  			buff.append(", ");
  		}
  		buff.append("}");
  		return buff.toString();
  	}
}
