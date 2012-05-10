/**
 * A basic exception used within RTMPSClient for notifying connection problems
 * 
 * @author Gabriel Van Eyck
 */
public class ConnectionException extends Exception
{
	private static final long serialVersionUID = -1117203256388896974L;

	public ConnectionException(String message)
	{
		super(message);
	}

	public ConnectionException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
