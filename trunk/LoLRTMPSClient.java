import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
	private static final int port = 2099;
	private String server;

	/** Login information */
	private String loginQueue;
	private String user;
	private String pass;

	/** Secondary login information */
	private String clientVersion;
	private String locale;
	private String ipAddress;

	/** Connection information */
	private String authToken;
	private String sessionToken;
	private int accountID;
	
	/** Heartbeat */
	private HeartbeatThread hb;
	
	/**
	 * A basic test for LoLRTMPSClient
	 * 
	 * @param args Unused
	 */
	public static void main(String[] args)
	{
		String user = "qweasn";
		String pass = "123qwe";
		
		String summoner = "Jabe";
		
		LoLRTMPSClient client = new LoLRTMPSClient("NA", "1.59.FOOBAR", user, pass);

		try
		{
			int id;
			client.connectAndLogin();
			
			// Synchronous invoke
			id = client.writeInvoke("summonerService", "getSummonerByName", new Object[] { summoner });
			System.out.println(client.getResult(id));
			
			// Asynchronous invoke
			client.writeInvokeWithCallback("summonerService", "getSummonerByName", new Object[] { summoner },
					new Callback()
					{
						public void callback(TypedObject result)
						{
							System.out.println(result);
						}
					});
			
			client.join(); // Wait for all current requests to finish
			
			client.close();
		}
		catch (Exception e)
		{
			System.out.println(client.lastDecode);
			e.printStackTrace();
		}
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
		else
		{
			System.out.println("Invalid server: " + region);
			System.out.println("Valid servers are: NA, EUW, EUN");
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
		getIPAddress();
		getAuthToken();

		connect(); // Will throw the connection exception

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
		int id = writeInvoke("loginService", "login", new Object[] { body });
		
		// Read relevant data
		result = getResult(id);
		body = result.getTO("body").getTO("body");
        sessionToken = (String)body.get("token");
        accountID = ((Double)body.getTO("accountSummary").get("accountId")).intValue();

		// Login 2
        byte[] encbuff = null;
		encbuff = (user.toLowerCase() + ":" + sessionToken).getBytes("UTF-8");

        body = wrapBody(Base64.encodeBytes(encbuff), "auth", 8);
        body.type = "flex.messaging.messages.CommandMessage";

		id = nextInvokeID();
		byte[] data = aec.encodeInvoke(id, body);
		out.write(data, 0, data.length);
		out.flush();
		
		// Read result (and discard)
		result = getResult(id);
		
		// Start the heartbeat
		hb = new HeartbeatThread();
	}

	/**
	 * Calls Riot's IP address informer
	 * 
	 * throws IOException
	 */
	private void getIPAddress() throws IOException
	{
		String response = readURL("http://ll.leagueoflegends.com/services/connection_info");
		ipAddress = response.substring(response.indexOf(":") + 2, response.length() - 2);
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
		// {"backlog":
		// "8","387":"3d23b","388":"3cba5","389":"3c9ac","390":"3d10a","391":"3cc67"}

		// THEN
		// login-queue/rest/queue/authToken/USERHERE

		// Then optionally
		// login-queue/rest/queue/cancelQueue/USERHERE

		// Initial authToken request
		String payload = "user=" + user + ",password=" + pass;
		String query = "payload=" + URLEncoder.encode(payload, "ISO-8859-1");

		URL url = new URL("https://lq.na1.lol.riotgames.com/login-queue/rest/queue/authenticate");
		HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();

		connection.setDoOutput(true);
		connection.setRequestMethod("POST");

		// Open up the output stream of the connection
		DataOutputStream output = new DataOutputStream(connection.getOutputStream());

		// Write the POST data
		output.writeBytes(query);
		output.close();

		// Read the response
		String response = readAll(connection.getInputStream());

		// Handle login queue
		int idx;
		if (!response.contains("token"))
		{
			// node is our login queue
			idx = response.indexOf("node");
			String node = response.substring(idx + 6, response.indexOf(",", idx + 6));

			// champ is the name of our login queue
			idx = response.lastIndexOf("champ");
			String champ = response.substring(idx + 8, response.indexOf("\"", idx + 8));

			// id is our ticket in line
			idx = response.lastIndexOf("node\":" + node);
			idx = response.substring(0, idx).lastIndexOf("id");
			int id = Integer.parseInt(response.substring(idx + 4, response.indexOf(",", idx + 4)));

			// cur is the current ticket being processed
			idx = response.indexOf("current", idx);
			int cur = Integer.parseInt(response.substring(idx + 9, response.indexOf("}", idx + 9)));

			// delay is how often the queue status updates
			idx = response.indexOf("delay");
			int sleeptime = Integer.parseInt(response.substring(idx + 7, response.indexOf(",", idx + 7)));

			// rate is how many people are processed every queue update (I think?)
			idx = response.indexOf("rate");
			int rate = Integer.parseInt(response.substring(idx + 6, response.indexOf(",", idx + 6)));

			// Request the queue status until there's only 'rate' left to go
			while (cur + rate < id)
			{
				try { Thread.sleep(sleeptime); } catch (InterruptedException e) { } // Sleep until the queue updates
				response = readURL(loginQueue + "login-queue/rest/queue/ticker/" + champ);
				idx = response.indexOf(node) + 3 + node.length();
				cur = hexToInt(response.substring(idx, response.indexOf("\"", idx)));
			}

			// Then try getting our token repeatedly
			response = readURL(loginQueue + "login-queue/rest/queue/authToken/" + user);
			while (!response.contains("token"))
			{
				try { Thread.sleep(sleeptime / 10); } catch (InterruptedException e) { }
				response = readURL(loginQueue + "login-queue/rest/queue/authToken/" + user);
			}
		}

		// Read the auth token
		idx = response.indexOf("token") + 8;
		authToken = response.substring(idx, idx + 36);
	}

	/**
	 * Reads all data available at a given URL
	 * 
	 * @param url The URL to read
	 * @return All data present at the given URL
	 * @throws IOException
	 */
	private String readURL(String url) throws IOException
	{
		try
		{
			return readAll(new URL(url).openStream());
		}
		catch (MalformedURLException e)
		{
			// Shouldn't happen
			e.printStackTrace();
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
	class HeartbeatThread extends Thread
	{
		private boolean running;
		private int heartbeat;
		private SimpleDateFormat sdf = new SimpleDateFormat("ddd MMM d yyyy HH:mm:ss 'GMTZ'");
		
		public HeartbeatThread()
		{
			this.heartbeat = 1;
			this.start();
		}
		
		public void die()
		{
			running = false;
		}
		
		public void run()
		{
			running = true;
			while (running)
			{
				try
				{
					writeInvoke("loginService", "performLCDSHeartBeat",
							new Object[] { accountID, sessionToken, heartbeat, sdf.format(new Date()) });

					heartbeat++;
					
					Thread.sleep(120000);
				}
				catch (Exception e) { } // Ignored for now
			}
		}
	}
}
