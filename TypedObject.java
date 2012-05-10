import java.util.HashMap;

/**
 * A map that has a type, used to represent an object
 * 
 * @author Gabriel Van Eyck
 */
public class TypedObject extends HashMap<String, Object>
{
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

	public String toString()
	{
		return type + ":" + super.toString();
	}
}
