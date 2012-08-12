import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

/**
 * A very basic RTMPS client for connecting to League of Legends
 * 
 * @author Gabriel Van Eyck
 */
public class LoLRTMPSClient extends RTMPSClient
{
	/** Server information */
	private static final int port = 2099; // Must be 2099
	private String server;
	private String region;

	/** Login information */
	private boolean loggedIn = false;
	private String loginQueue;
	private String user;
	private String pass;

	/** Secondary login information */
	private String clientVersion;
	private String ipAddress;
	private String locale;

	/** Connection information */
	private String authToken;
	private String sessionToken;
	private int accountID;

	/**
	 * A basic test for LoLRTMPSClient
	 * 
	 * @param args Unused
	 */
	public static void main(String[] args)
	{
		String user = "qweasm";
		String pass = "123qwe";

		String summoner = "Jabe";

		LoLRTMPSClient client = new LoLRTMPSClient("NA", "1.62.FOOBAR", user, pass);
		//client.debug = true;

		try
		{
			int id;
			client.connectAndLogin();
			client.reconnect();

			// Synchronous invoke
			id = client.invoke("summonerService", "getSummonerByName", new Object[] { summoner });
			System.out.println(client.getResult(id));

			// Asynchronous invoke
			client.invokeWithCallback("summonerService", "getSummonerByName", new Object[] { summoner },
					new Callback()
					{
						public void callback(TypedObject result)
						{
							System.out.println(result);
						}
					});

			client.join(); // Wait for all current requests to finish
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		client.close();
	}

	/**
	 * Hidden constructor
	 */
	private LoLRTMPSClient()
	{
		super();
	}

	/**
	 * Sets up a RTMPSClient for this client to use
	 * 
	 * @param region The region to connect to (NA/EUW/EUN)
	 * @param clientVersion The current client version for LoL (top left of client)
	 * @param user The user to login as
	 * @param pass The user's password
	 */
	public LoLRTMPSClient(String region, String clientVersion, String user, String pass)
	{
		region = region.toUpperCase();
		
		this.region = region;
		this.clientVersion = clientVersion;
		this.user = user;
		this.pass = pass;

		if (region.equals("NA"))
		{
			this.server = "prod.na1.lol.riotgames.com";
			this.loginQueue = "https://lq.na1.lol.riotgames.com/";
			this.locale = "en_US";
		}
		else if (region.equals("EUW"))
		{
			this.server = "prod.eu.lol.riotgames.com";
			this.loginQueue = "https://lq.eu.lol.riotgames.com/";
			this.locale = "en_GB";
		}
		else if (region.equals("EUN"))
		{
			this.server = "prod.eun1.lol.riotgames.com";
			this.loginQueue = "https://lq.eun1.lol.riotgames.com/";
			this.locale = "en_GB";
		}
		else if (region.equals("KR"))
		{
			this.server = "prod.kr.lol.riotgames.com";
			this.loginQueue = "https://lq.kr.lol.riotgames.com/";
			this.locale = "ko_KR";
		}
		else if (region.equals("BR"))
		{
			this.server = "prod.br.lol.riotgames.com";
			this.loginQueue = "https://lq.br.lol.riotgames.com/";
			this.locale = "pt_BR";
		}
		else if (region.equals("PBE"))
		{
			this.server = "prod.pbe1.lol.riotgames.com";
			this.loginQueue = "https://lq.pbe1.lol.riotgames.com/";
			this.locale = "en_US";
		}
		else
		{
			System.out.println("Invalid region: " + region);
			System.out.println("Valid region are: NA, EUW, EUN, KR, BR, PBE");
			System.exit(0);
		}

		setConnectionInfo(this.server, port, "", "app:/mod_ser.dat", null);
	}

	/**
	 * Connects and logs in using the information previously provided
	 * 
	 * @throws IOException
	 */
	public void connectAndLogin() throws IOException
	{
		connect();
		login();
	}

	/**
	 * Logs into Riot's servers
	 * 
	 * @throws IOException
	 */
	public void login() throws IOException
	{
		getIPAddress();
		getAuthToken();

		TypedObject result, body;

		// Login 1
		body = new TypedObject("com.riotgames.platform.login.AuthenticationCredentials");
		body.put("password", pass);
		body.put("clientVersion", clientVersion);
		body.put("ipAddress", ipAddress);
		body.put("securityAnswer", null);
		body.put("partnerCredentials", null);
		body.put("locale", locale);
		body.put("domain", "lolclient.lol.riotgames.com");
		body.put("oldPassword", null);
		body.put("username", user);
		body.put("authToken", authToken);
		int id = invoke("loginService", "login", new Object[] { body });

		// Read relevant data
		result = getResult(id);
		if (result.get("result").equals("_error"))
			throw new IOException(getErrorMessage(result));
		
		body = result.getTO("data").getTO("body");
		sessionToken = body.getString("token");
		accountID = body.getTO("accountSummary").getInt("accountId");
		
		// Login 2
		byte[] encbuff = null;
		encbuff = (user.toLowerCase() + ":" + sessionToken).getBytes("UTF-8");

		body = wrapBody(Base64.encodeBytes(encbuff), "auth", 8);
		body.type = "flex.messaging.messages.CommandMessage";

		id = invoke(body);
		result = getResult(id); // Read result (and discard)
		
		// Subscribe to the necessary items
		body = wrapBody(new Object[] { new TypedObject() }, "messagingDestination", 0);
		body.type = "flex.messaging.messages.CommandMessage";
		TypedObject headers = body.getTO("headers");
		//headers.put("DSRemoteCredentialsCharset", null); // unneeded
		//headers.put("DSRemoteCredentials", "");
		
		// bc
		headers.put("DSSubtopic", "bc");
		body.put("clientId", "bc-" + accountID);
		id = invoke(body);
		result = getResult(id); // Read result and discard
		
		// cn
		headers.put("DSSubtopic", "cn-" + accountID);
		body.put("clientId", "cn-" + accountID);
		id = invoke(body);
		result = getResult(id); // Read result and discard

		// gn
		headers.put("DSSubtopic", "gn-" + accountID);
		body.put("clientId", "gn-" + accountID);
		id = invoke(body);
		result = getResult(id); // Read result and discard

		// Start the heartbeat
		new HeartbeatThread();
		
		loggedIn = true;

		System.out.println("Connected to " + region);
	}
	
	/**
	 * Closes the connection
	 */
	public void close()
	{
		loggedIn = false;
		
		if (out != null)
		{
			// And attempt to logout, but don't care if we fail
			try
			{
				int id = invoke("loginService", "logout", new Object[] { authToken });
				join(id);
			}
			catch (IOException e)
			{
				// Ignored
			}
		}
		
		super.close();
	}
	
	/**
	 * Additional reconnect steps for logging in after a reconnect
	 */
	public void reconnect()
	{
		// Socket/RTMP reconnect
		super.reconnect();

		// Then login
		while (!isLoggedIn())
		{
			try
			{
				login();
			}
			catch (IOException e)
			{
				System.err.println("Error when reconnecting: ");
				e.printStackTrace(); // For debug purposes

				sleep(5000);
				super.reconnect(); // Need to reconnect again here
			}
		} 
	}
	
	/**
	 * Returns the login state
	 * 
	 * @return True if passed login queue and commands
	 */
	public boolean isLoggedIn()
	{
		return loggedIn;
	}
	
	/**
	 * Extracts the rootCause from an error message
	 *  
	 * @param message The packet result
	 * @return The error message
	 */
	public String getErrorMessage(TypedObject message)
	{
		// Works for clientVersion
		return (debug ? message.toString() : message.getTO("data").getTO("rootCause").getString("message"));
	}

	/**
	 * Calls Riot's IP address informer
	 * 
	 * @throws IOException
	 */
	private void getIPAddress() throws IOException
	{
		// Don't need to retrieve IP address on reconnect (probably)
		if (ipAddress != null)
			return;
		
		String response = readURL("http://ll.leagueoflegends.com/services/connection_info");
		TypedObject result = (TypedObject)JSON.parse(response);
		ipAddress = result.getString("ip_address");
	}

	/**
	 * Gets an authentication token for logging into Riot's servers
	 * 
	 * @throws IOException
	 */
	private void getAuthToken() throws IOException
	{
		// login-queue/rest/queue/authenticate
		// {"rate":60,"token":"d9a18f08-8159-4c27-9f3a-7927462b5150","reason":"login_rate","status":"LOGIN","delay":10000,"user":"USERHERE"}
		// --- OR ---
		// {"node":388,"vcap":20000,"rate":30,
		// "tickers":[
		// {"id":267284,"node":388,"champ":"Soraka","current":248118}, CHAMP MATTERS
		// {"id":266782,"node":389,"champ":"Soraka","current":247595},
		// {"id":269287,"node":390,"champ":"Soraka","current":249444},
		// {"id":270005,"node":387,"champ":"Soraka","current":249735},
		// {"id":267732,"node":391,"champ":"Soraka","current":248190}
		// ],
		// "backlog":4,"reason":"login_rate","status":"QUEUE","champ":"Soraka","delay":10000,"user":"USERHERE"}

		// IF QUEUE
		// login-queue/rest/queue/ticker/CHAMPHERE
		// {"backlog":"8","387":"3d23b","388":"3cba5","389":"3c9ac","390":"3d10a","391":"3cc67"}

		// THEN
		// login-queue/rest/queue/authToken/USERHERE

		// Then optionally
		// login-queue/rest/queue/cancelQueue/USERHERE

		// Initial authToken request
		String payload = "user=" + user + ",password=" + pass;
		String query = "payload=" + URLEncoder.encode(payload, "ISO-8859-1");

		URL url = new URL(loginQueue + "login-queue/rest/queue/authenticate");
		HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();

		connection.setDoOutput(true);
		connection.setRequestMethod("POST");

		// Open up the output stream of the connection
		DataOutputStream output = new DataOutputStream(connection.getOutputStream());

		// Write the POST data
		output.writeBytes(query);
		output.close();

		// Read the response
		String response;
		TypedObject result;
		try
		{
			//{"rate":0,"reason":"account_banned","status":"FAILED","delay":10000,"banned":7647952951000}
			response = readAll(connection.getInputStream());
			result = (TypedObject)JSON.parse(response);
		}
		catch (IOException e)
		{
			System.err.println("Incorrect username or password");
			throw e;
		}

		// Handle login queue
		if (!result.containsKey("token"))
		{
			int node = result.getInt("node"); // Our login queue ID
			String nodeStr = "" + node;
			String champ = result.getString("champ"); // The name of our login queue
			int rate = result.getInt("rate"); // How many tickets are processed every queue update
			int delay = result.getInt("delay"); // How often the queue status updates
			
			int id = 0;
			int cur = 0;
			Object[] tickers = result.getArray("tickers");
			for (Object o : tickers)
			{
				TypedObject to = (TypedObject)o;
				
				// Find our queue
				int tnode = to.getInt("node");
				if (tnode != node)
					continue;
				
				id = to.getInt("id"); // Our ticket in line
				cur = to.getInt("current"); // The current ticket being processed
				break;
			}
			
			// Let the user know
			System.out.println("In login queue for " + region + ", #" + (id - cur) + " in line");

			// Request the queue status until there's only 'rate' left to go
			while (id - cur > rate)
			{
				sleep(delay); // Sleep until the queue updates
				response = readURL(loginQueue + "login-queue/rest/queue/ticker/" + champ);
				result = (TypedObject)JSON.parse(response);
				if (result == null)
					continue;
			
				cur = hexToInt(result.getString(nodeStr));
				System.out.println("In login queue for " + region + ", #" + (int)Math.max(1, id - cur) + " in line");
			}

			// Then try getting our token repeatedly
			response = readURL(loginQueue + "login-queue/rest/queue/authToken/" + user.toLowerCase());
			result = (TypedObject)JSON.parse(response);
			while (response == null || !result.containsKey("token"))
			{
				sleep(delay / 10);
				response = readURL(loginQueue + "login-queue/rest/queue/authToken/" + user.toLowerCase());
				result = (TypedObject)JSON.parse(response);
			}
		}

		// Read the auth token
		authToken = result.getString("token");
	}
	
	/**
	 * Reads all data available at a given URL
	 * 
	 * @param url The URL to read
	 * @return All data present at the given URL
	 * @throws IOException
	 */
	private String readURL(String url)
	{
		try
		{
			return readAll(new URL(url).openStream());
		}
		catch (MalformedURLException e)
		{
			// Should never happen
			e.printStackTrace();
			return null;
		}
		catch (IOException e)
		{
			// Only happens when we try to get our token too fast
			return null;
		}
	}

	/**
	 * Reads all data from the given InputStream
	 * 
	 * @param in The InputStream to read from
	 * @return All data from the given InputStream
	 * @throws IOException
	 */
	private String readAll(InputStream in) throws IOException
	{
		StringBuilder ret = new StringBuilder();

		// Read in each character until end-of-stream is detected
		int c;
		while ((c = in.read()) != -1)
			ret.append((char)c);

		return ret.toString();
	}

	/**
	 * Converts a hex string to an integer
	 * 
	 * @param hex The hex string
	 * @return The equivalent integer
	 */
	private int hexToInt(String hex)
	{
		int total = 0;
		for (int i = 0; i < hex.length(); i++)
		{
			char c = hex.charAt(i);
			if (c >= '0' && c <= '9')
				total = total * 16 + c - '0';
			else
				total = total * 16 + c - 'a' + 10;
		}

		return total;
	}

	/**
	 * Executes a LCDSHeartBeat every 2 minutes
	 */
	class HeartbeatThread
	{
		private Thread curThread;
		private int heartbeat;
		private SimpleDateFormat sdf = new SimpleDateFormat("ddd MMM d yyyy HH:mm:ss 'GMTZ'");

		public HeartbeatThread()
		{
			this.heartbeat = 1;
			curThread = new Thread() {
	            public void run() {
	            	beatHeart(this);
	            }
	        };
	        curThread.setName("LoLRTMPSClient (HeartbeatThread)");
	        curThread.setDaemon(true);
	        curThread.start();
		}

		private void beatHeart(Thread thread)
		{
			while (curThread == thread)
			{
				try
				{
					long hbTime = System.currentTimeMillis();
					
					int id = invoke("loginService", "performLCDSHeartBeat", new Object[] { accountID, sessionToken, heartbeat, sdf.format(new Date()) });
					cancel(id); // Ignore result for now

					heartbeat++;

					// Quick sleeps to shutdown the heartbeat quickly on a reconnect
					while (curThread == thread && System.currentTimeMillis() - hbTime < 120000)
						sleep(100);
				}
				catch (Exception e)
				{
					if (!reconnecting)
						doReconnect();
				}
			}
		}
	}
}
