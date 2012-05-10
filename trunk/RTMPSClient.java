import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.SSLSocketFactory;

/**
 * A very basic RTMPS client
 * 
 * @author Gabriel Van Eyck
 */
public class RTMPSClient
{
	/** Server information */
	protected String server;
	protected int port;
	protected String app;
	protected String swfUrl;
	protected String pageUrl;

	/** Connection information */
	protected String DSId;

	/** Socket and streams */
	protected Socket sslsocket;
	protected InputStream in;
	protected DataOutputStream out;
	protected RTMPPacketReader pr;

	/** State information */
	protected boolean connected = false;

	/** Used for generating handshake */
	protected Random rand = new Random();

	/** Encoder */
	protected AMF3Encoder aec = new AMF3Encoder();
	
	/**
	 * A simple test for doing the basic RTMPS connection to Riot
	 * 
	 * @param args Unused
	 */
	public static void main(String[] args)
	{
		RTMPSClient client = new RTMPSClient("prod.na1.lol.riotgames.com", 2099, "", "app:/mod_ser.dat", null);
		try
		{
			client.connect();
			if (client.isConnected())
				System.out.println("Success");
			client.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Hidden constructor
	 */
	protected RTMPSClient()
	{
	}
	
	/**
	 * Sets up the client with the given parameters
	 * 
	 * @param server The RTMPS server address
	 * @param port The RTMPS server port
	 * @param app The app to use in the connect call
	 * @param swfUrl The swf URL to use in the connect call
	 * @param pageUrl The page URL to use in the connect call
	 */
	public RTMPSClient(String server, int port, String app, String swfUrl, String pageUrl)
	{
		this.server = server;
		this.port = port;

		this.app = app;
		this.swfUrl = swfUrl;
		this.pageUrl = pageUrl;
	}
	
	/**
	 * Sets up the client with the given parameters
	 * 
	 * @param server The RTMPS server address
	 * @param port The RTMPS server port
	 * @param app The app to use in the connect call
	 * @param swfUrl The swf URL to use in the connect call
	 * @param pageUrl The page URL to use in the connect call
	 */
	public void setConnectionInfo(String server, int port, String app, String swfUrl, String pageUrl)
	{
		this.server = server;
		this.port = port;

		this.app = app;
		this.swfUrl = swfUrl;
		this.pageUrl = pageUrl;
	}

	/**
	 * Closes the connection
	 */
	public void close()
	{
		pr.die();
		
		try
		{
			sslsocket.close();
		}
		catch (IOException e)
		{
			// Do nothing
			e.printStackTrace();
		}
	}

	/**
	 * Attempts to connect given the previous connection information
	 * 
	 * @throws ConnectionException
	 * @throws IOException
	 */
	public void connect() throws ConnectionException, IOException
	{
		sslsocket = SSLSocketFactory.getDefault().createSocket(server, 2099);
		in = new BufferedInputStream(sslsocket.getInputStream());
		out = new DataOutputStream(sslsocket.getOutputStream());

		doHandshake();

		// Start reading responses
		pr = new RTMPPacketReader(in);

		// Handle preconnect Messages?
		// -- 02 | 00 00 00 | 00 00 05 | 06 00 00 00 00 | 00 03 D0 90 02

		// Connect
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("app", app);
		params.put("flashVer", "WIN 10,1,85,3");
		params.put("swfUrl", swfUrl);
		params.put("tcUrl", "rtmps://" + server + ":" + port);
		params.put("fpad", false);
		params.put("capabilities", 239);
		params.put("audioCodecs", 3191);
		params.put("videoCodecs", 252);
		params.put("videoFunction", 1);
		params.put("pageUrl", pageUrl);
		params.put("objectEncoding", 3);

		try
		{
			byte[] connect = aec.encodeConnect(params);
			
			out.write(connect, 0, connect.length);
			out.flush();
	
			TypedObject result = pr.getPacket(1);
			TypedObject body = (TypedObject)result.get("body");
			DSId = (String)body.get("id");
			
			connected = true;
		}
		catch (Exception e)
		{
			throw new ConnectionException("Error encoding or decoding", e);
		}
	}

	/**
	 * Executes a full RTMP handshake
	 * 
	 * @throws ConnectionException
	 * @throws IOException
	 */
	private void doHandshake() throws ConnectionException, IOException
	{
		// C0
		byte C0 = 0x03;
		out.write(C0);

		// C1
		long timestampC1 = System.currentTimeMillis();
		byte[] randC1 = new byte[1528];
		rand.nextBytes(randC1);

		out.writeInt((int)timestampC1);
		out.writeInt(0);
		out.write(randC1, 0, 1528);
		out.flush();

		// S0
		byte S0 = (byte)in.read();
		if (S0 != 0x03)
			throw new ConnectionException("Server returned incorrect version in handshake: " + S0);

		// S1
		byte[] S1 = new byte[1536];
		in.read(S1, 0, 1536);

		// C2
		long timestampS1 = System.currentTimeMillis();
		out.write(S1, 0, 4);
		out.writeInt((int)timestampS1);
		out.write(S1, 8, 1528);
		out.flush();

		// S2
		byte[] S2 = new byte[1536];
		in.read(S2, 0, 1536);

		// Validate handshake
		boolean valid = true;
		for (int i = 8; i < 1536; i++)
		{
			if (randC1[i - 8] != S2[i])
			{
				valid = false;
				break;
			}
		}

		if (!valid)
			throw new ConnectionException("Handshake was not valid");
	}
	
	/**
	 * Returns the connection status
	 * 
	 * @return True if connected
	 */
	public boolean isConnected()
	{
		return (connected && pr.running);
	}
	
	/**
	 * Removes and returns a result for a given invoke ID if it's ready
	 * Returns null otherwise
	 * 
	 * @param id The invoke ID
	 * @return The invoke's result or null
	 */
	public TypedObject peekResult(int id)
	{
		return pr.peekPacket(id);
	}

	/**
	 * Blocks and waits for the invoke's result to be ready, then removes and returns it
	 * 
	 * @param id The invoke ID
	 * @return The invoke's result
	 */
	public TypedObject getResult(int id)
	{
		return pr.getPacket(id);
	}

	/**
	 * Reads RTMP packets from a stream
	 * 
	 * @author Gabriel Van Eyck
	 */
	class RTMPPacketReader extends Thread
	{
		/** The status of the packet reader */
		public boolean running;

		/** The stream to read from */
		private InputStream in;

		/** Current packet information and buffer */
		private int dataSize;
		private int messageType;
		private List<Byte> dataBuffer = new ArrayList<Byte>();

		/** Map of decoded packets */
		private Map<Integer, TypedObject> results = Collections.synchronizedMap(new HashMap<Integer, TypedObject>());
		
		/** The AMF3 decoder */
		private AMF3Decoder adc = new AMF3Decoder();

		/**
		 * Starts a packet reader on the given stream
		 * 
		 * @param stream The stream to read packets from
		 */
		public RTMPPacketReader(InputStream stream)
		{
			this.in = stream;
			this.running = true;
			this.start();
		}
		
		/**
		 * Stops the packet reader
		 */
		public void die()
		{
			running = false;
		}
		
		/**
		 * Removes and returns a result for a given invoke ID if it's ready
		 * Returns null otherwise
		 * 
		 * @param id The invoke ID
		 * @return The invoke's result or null
		 */
		public TypedObject peekPacket(int id)
		{
			if (results.containsKey(id))
			{
				TypedObject ret = results.get(id);
				results.remove(id);
				return ret;
			}
			return null;
		}

		/**
		 * Blocks and waits for the invoke's result to be ready, then removes and returns it
		 * 
		 * @param id The invoke ID
		 * @return The invoke's result
		 */
		public TypedObject getPacket(int id)
		{
			while (!results.containsKey(id) && running)
			{
				try
				{
					Thread.sleep(10);
				}
				catch (Exception e)
				{
				}
			}

			if (!running)
				return null;

			return results.remove(id);
		}

		/**
		 * The main loop for the packet reader
		 */
		public void run()
		{
			try
			{
				dataSize = -1;
				while (running)
				{
					if (dataSize == -1)
					{
						// Read header to find out how much more we need to read before decoding
						byte[] header = new byte[12];
						in.read(header, 0, 12);
						messageType = header[7];

						dataSize = 0;
						for (int i = 0; i < 3; i++)
							dataSize = dataSize * 256 + (header[4 + i] < 0 ? header[4 + i] + 256 : header[4 + i]);

						for (byte b : header)
							dataBuffer.add(b);
					}
					else
					{
						// Read rest of packet
						int pos = 0;
						while (pos < dataSize)
						{
							dataBuffer.add((byte)in.read());
							if (pos != 0 && pos % 128 == 0) // Read extra byte for chunking
								dataBuffer.add((byte)in.read());
							pos++;
						}

						// Switch to byte array for decoding
						byte[] temp = new byte[dataBuffer.size()];
						for (int i = 0; i < temp.length; i++)
							temp[i] = dataBuffer.get(i);

						dataSize = -1;
						dataBuffer = new ArrayList<Byte>();

						// Decode if necessary
						TypedObject result = null;
						if (messageType == 0x14) // Connect
							result = adc.decodeConnect(temp);
						else if (messageType == 0x11) // Invoke
							result = adc.decodeInvoke(temp);
						else // Discard rest
							continue;

						results.put((Integer)result.get("invokeId"), result);
					}
				}
			}
			catch (Exception e)
			{
				// Assume the stream was closed by design or that the error was already handled
				//System.out.println("Error while reading from stream");
				//e.printStackTrace();
			}

			running = false;
		}
	}
}
