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
	 * Initializes the type of the object
	 * null type implies a dynamic class (used for headers)
	 * 
	 * @param type The type of the object
	 */
	public TypedObject(String type)
	{
		this.type = type;
	}

	/**
	 * Creates a flex.messaging.io.ArrayCollection in the structure that
	 * the encoder expects
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
	 * @param key The key of the TypedObject
	 * @return The TypedObject
	 */
	public TypedObject getTO(String key)
	{
		return (TypedObject)get(key);
	}

	public String toString()
	{
		return type + ":" + super.toString();
	}
}
