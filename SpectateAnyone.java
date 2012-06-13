import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple program that allows a player to spectate anyone playing a game
 * (rather than just people on your friends list) 
 * 
 * @author Gabriel Van Eyck
 */
public class SpectateAnyone
{
	/**
	 * Wrapper to get a line of text from System.in
	 * 
	 * @return The next line of text from System.in
	 */
	public static String getInput()
	{
		StringBuilder buffer = new StringBuilder();
		int c;
		
		try
		{
			while ((c = System.in.read()) != -1)
			{
				if (c == '\r')
					continue;
				if (c == '\n')
					break;
				
				buffer.append((char)c);
			}
		}
		catch (IOException e) { }
		
		return buffer.toString();
	}

	/**
	 * Entry point for the application
	 * 
	 * @param args Unused
	 */
	public static void main(String[] args)
	{
		// Read in the config
		System.out.println("Loading config.txt");
		File conf = new File("config.txt");
		Map<String, String> params = new HashMap<String, String>();
		
		// Parse if exists
		if (conf.exists())
		{
			try
			{
				String line;
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(conf), "UTF-8"));
				while ((line = in.readLine()) != null)
				{
					String[] parts = line.split("=");
					if (parts.length != 2)
						continue;
					
					params.put(parts[0].trim(), parts[1].trim());
				}
				in.close();
			}
			catch (IOException e)
			{
				System.out.println("Encountered an error when parsing config.txt:");
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("config.txt did not exist.");
			System.out.println("Please enter the following information.");
			System.out.println();
		}
		
		// Check for completeness
		if (!(params.containsKey("user") && 
			  params.containsKey("pass") && 
			  params.containsKey("lollocation") && 
			  params.containsKey("version") && 
			  params.containsKey("region")))
		{
			// Removed for now
		}

		// Get missing information
		boolean newinfo = false;
		if (!params.containsKey("lollocation"))
		{
			System.out.print("LoL install location (e.g. C:\\Riot Games\\League of Legends\\): ");
			params.put("lollocation", getInput());
			newinfo = true;
		}

		if (!params.containsKey("region"))
		{
			System.out.print("Region (NA/EUW/EUN): ");
			params.put("region", getInput().toUpperCase());
			newinfo = true;
		}

		if (!params.containsKey("version"))
		{
			System.out.println("Client version can be found at the top left of the real client");
			System.out.print("Client version (e.g. 1.60.foo): ");
			params.put("version", getInput());
			newinfo = true;
		}

		if (!params.containsKey("user"))
		{
			System.out.print("User: ");
			params.put("user", getInput());
			newinfo = true;
		}

		if (!params.containsKey("pass"))
		{
			System.out.print("Pass: ");
			params.put("pass", getInput());
		}
		
		// Set up config.txt if needed
		if (newinfo)
		{
			try
			{
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conf), "UTF-8"));
				out.write("lollocation=" + params.get("lollocation") + "\r\n");
				out.write("user=" + params.get("user") + "\r\n");
				//out.write("pass=" + params.get("pass") + "\r\n");
				out.write("version=" + params.get("version") + "\r\n");
				out.write("region=" + params.get("region") + "\r\n");
				out.close();
			}
			catch (IOException e)
			{
				System.out.println("Encountered an error when creating config.txt:");
				e.printStackTrace();
			}
		}
		
		// Set the region used for the game executable
		String region = params.get("region");
		if (region.equals("NA"))
			region = "NA1";
		else if (region.equals("EUW"))
			region = "EUW1";
		else if (region.equals("EUN"))
			region = "EUN1";
		
		// Fix location if necessary
		String loc = params.get("lollocation");
		loc.replace("/", "\\");
		if (!loc.endsWith("\\"))
			loc = loc + "\\";
		params.put("lollocation", loc);
		
		// Figure out the right version of the client to run
		File gameDir = new File(loc + "RADS\\solutions\\lol_game_client_sln\\releases\\");
		File clientDir = new File(loc + "RADS\\projects\\lol_air_client\\releases\\");
		if (!gameDir.exists() || !clientDir.exists())
		{
			System.err.println("The installation location does not appear to be valid, make sure it is correct");
			System.exit(0);
		}
		
		int maxGame = 0;
		for (File f : gameDir.listFiles())
		{
			String[] parts = f.getName().split("\\.");
			if (parts.length != 4)
				continue;
			
			int ver = Integer.parseInt(parts[3]);
			if (ver > maxGame)
				maxGame = ver;
		}
		
		int maxClient = 0;
		for (File f : clientDir.listFiles())
		{
			String[] parts = f.getName().split("\\.");
			if (parts.length != 4)
				continue;
			
			int ver = Integer.parseInt(parts[3]);
			if (ver > maxClient)
				maxClient = ver;
		}
		
		// Connect
		System.out.println("Connecting...");
		System.out.println();
		LoLRTMPSClient client = new LoLRTMPSClient(params.get("region"), params.get("version"), params.get("user"), params.get("pass"));
		try
		{
			client.connectAndLogin();
		}
		catch (IOException e)
		{
			System.out.println("Failed to connect");
			if (e.getMessage() != null && e.getMessage().contains("403"))
				System.out.println("Incorrect user or pass");
			e.printStackTrace();
			System.exit(0);
		}
		
		// Spectate people
		System.out.println();
		System.out.println("Hit enter without typing anything to exit");
		System.out.println("!file.txt will print out the summoners from 'file.txt' that can be observed");
		System.out.println("E.g. '!list.txt' will read from list.txt, use one summoner name per line");
		System.out.println();
		System.out.print("Spectate whom? ");
		String toSpec = getInput();
		while (toSpec.length() != 0)
		{
			// Check game status of all summoners in file
			if (toSpec.startsWith("!"))
			{
				String filename = toSpec.substring(1).trim();
				try
				{
					BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
					String line;
					while ((line = in.readLine()) != null)
					{
						line = line.trim();
						if (line.length() < 4)
							continue;
						
						// Invoke asynchronously
						final String name = line;
						client.writeInvokeWithCallback("gameService", "retrieveInProgressSpectatorGameInfo", new Object[] { name },
								new Callback()
								{
									public void callback(TypedObject result)
									{
										if (result.get("result").equals("_result"))
											System.out.println(name);
									}
								});
						
					}
					in.close();
					
					// Wait for all requests to finish;
					client.join();
				}
				catch (IOException e)
				{
					System.err.println("Error reading from " + filename + ":");
					e.printStackTrace();
				}
			}
			// Attempt to spectate a summoner
			else
			{
				try
				{
					// Get spectator info
					int id = client.writeInvoke("gameService", "retrieveInProgressSpectatorGameInfo", new Object[] { toSpec });
					TypedObject result = client.getResult(id);
					TypedObject data = result.getTO("data");
	
					// Handle errors
					if (result.get("result").equals("_error"))
					{
						if (data.getTO("rootCause").type.equals("com.riotgames.platform.messaging.UnexpectedServiceException"))
							System.err.println("No summoner found for " + toSpec);
						else if (data.getTO("rootCause").type.equals("com.riotgames.platform.game.GameNotFoundException"))
							System.err.println(toSpec + " is not currently in a game");
						else
						{
							System.err.println("Encountered an error when retrieving spectator information for " + toSpec + ":");
							System.err.println(data.getTO("rootCause").get("localizedMessage"));
						}
					}
					else
					{
						// Extract needed info
						TypedObject cred = data.getTO("body").getTO("playerCredentials");
						String ip = (String)cred.get("observerServerIp");
						int port = (Integer)cred.get("observerServerPort");
						String key = (String)cred.get("observerEncryptionKey");
						int gameID = ((Double)cred.get("gameId")).intValue();
						
						// Running the process directly causes it to hang, so create a batch file and run that
						BufferedWriter out = new BufferedWriter(new FileWriter("run.bat"));
						out.write(loc.substring(0, 2) + "\r\n"); // Change to drive if necessary
						out.write("cd \"" + loc + "RADS\\solutions\\lol_game_client_sln\\releases\\0.0.0." + maxGame + "\\deploy\"\r\n");
						out.write("start \"\" ");
						out.write("\"" + loc + "RADS\\solutions\\lol_game_client_sln\\releases\\0.0.0." + maxGame + "\\deploy\\League of Legends.exe\" ");
						out.write("8394 ");
						out.write("LoLLauncher.exe ");
						out.write("\"" + loc + "RADS\\projects\\lol_air_client\\releases\\0.0.0." + maxClient + "\\deploy\\LolClient.exe\" ");
						out.write("\"spectator " + ip + ":" + port + " " + key + " " + gameID + " " + region + "\"\r\n");
						out.flush();
						out.close();
						
						// Run (and make sure to consume output)
						Process game = Runtime.getRuntime().exec("run.bat");
						//StreamGobbler stdout = new StreamGobbler(game.getInputStream());
						//StreamGobbler stderr = new StreamGobbler(game.getErrorStream());
						new StreamGobbler(game.getInputStream());
						new StreamGobbler(game.getErrorStream());
						game.waitFor();
						
						// Print out any data
						//System.out.println("STDOUT");
						//System.out.println(stdout.getData());
						//System.out.println();
						//System.out.println("STDERR");
						//System.out.println(stderr.getData());
						
						// Delete temp file
						File temp = new File("run.bat");
						temp.delete();
					}
				}
				catch (IOException e)
				{
					System.err.println("Encountered an error when trying to retrieve spectate information for " + toSpec + ":");
					e.printStackTrace();
				}
				catch (InterruptedException e)
				{
					System.err.println("Something terrible happened");
					e.printStackTrace();
				}
			}
			
			// And loop
			System.out.print("Spectate whom? ");
			toSpec = getInput();
		}
		
		// And close the client
		client.close();
	}
}

class StreamGobbler extends Thread
{
	private InputStream in;
	private StringBuilder buffer = new StringBuilder();
	
	public StreamGobbler(InputStream in)
	{
		this.in = in;
		this.start();
	}
	
	public void run()
	{
		try
		{
			int c;
			
			while ((c = in.read()) != -1)
				buffer.append((char)c);
		}
		catch (IOException e)
		{
			// Ignored
		}
	}
	
	public String getData()
	{
		return buffer.toString();
	}
}
