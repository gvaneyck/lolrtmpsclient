import java.util.HashMap;

/**
 * A map that has a type, used to represent an object
 * 
 * @author Gabriel Van Eyck
 */
public class TypedObject extends HashMap<String, Object>
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

	public String toString()
	{
		if (type == null)
			return super.toString();
		else if (type.equals("flex.messaging.io.ArrayCollection"))
		{
			StringBuilder sb = new StringBuilder();
			Object[] data = (Object[])get("array");
			sb.append("ArrayCollection:[");
			for (int i = 0; i < data.length; i++)
			{
				sb.append(data[i]);
				if (i < data.length - 1)
					sb.append(", ");
			}
			sb.append(']');
			return sb.toString();
		}
		else
			return type + ":" + super.toString();
	}
}
