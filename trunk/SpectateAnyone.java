import java.awt.Container;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A simple program that allows a player to spectate anyone playing a game
 * (rather than just people on your friends list) 
 * 
 * @author Gabriel Van Eyck
 */
public class SpectateAnyone
{
	public static final JFrame f = new JFrame("Spectate Anyone!");
	
	public static final Label lblName = new Label("Name:");
	public static final JTextField txtName = new JTextField();
	public static final JButton btnName = new JButton();

	public static final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);

	public static final Label lblFile = new Label("Load List:");
	public static final JTextField txtFile = new JTextField();
	public static final JButton btnFile = new JButton("Check");

	public static final DefaultListModel lstModel = new DefaultListModel();
	public static final JList lstInGame = new JList(lstModel);
	public static final JScrollPane lstScroll = new JScrollPane(lstInGame);

	public static final JFileChooser fc = new JFileChooser(new File("."));
	
	public static int width = 350;
	public static int height = 250;
	
	public static LoLRTMPSClient client;
	public static Map<String, String> params;

	public static void main(String[] args)
	{
		setupFrame();
		setupClient();
	}
	
	public static void setupFrame()
	{
		// GUI settings
		lblName.setAlignment(Label.RIGHT);
		lblFile.setAlignment(Label.RIGHT);
		lstInGame.setLayoutOrientation(JList.VERTICAL_WRAP);
		lstInGame.setVisibleRowCount(-1);
		lstInGame.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// Initially grey out buttons
		btnName.setText("Connecting...");
		btnName.setEnabled(false);
		btnFile.setEnabled(false);
		
		// Add the items
		Container pane = f.getContentPane();
		pane.setLayout(null);

		pane.add(lblName);
		pane.add(txtName);
		pane.add(btnName);
		
		pane.add(sep);
		
		pane.add(lblFile);
		pane.add(txtFile);
		pane.add(btnFile);
		
		pane.add(lstScroll);

		// Layout everything
		doLayout();
		
		// Listeners
		txtName.addKeyListener(new KeyListener()
				{
					public void keyTyped(KeyEvent e) { }
					public void keyPressed(KeyEvent e) { }
	
					public void keyReleased(KeyEvent e)
					{
						if (e.getKeyCode() == KeyEvent.VK_ENTER && btnName.isEnabled())
							handleSpectate();
					}
				});
		
		btnName.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						handleSpectate();
					}
				});
		
		txtFile.addFocusListener(new FocusListener()
				{
					public void focusLost(FocusEvent e) { }

					public void focusGained(FocusEvent e)
					{
						setFile();
					}
				});

		btnFile.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						
						loadFile();
					}
				});
		
		lstInGame.addMouseListener(new MouseListener()
				{
					public void mousePressed(MouseEvent e) { }
					public void mouseReleased(MouseEvent e) { }
					public void mouseEntered(MouseEvent e) { }
					public void mouseExited(MouseEvent e) { }
		
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2)
							handleSpectate();
					}
				});

		lstInGame.addKeyListener(new KeyListener()
				{
					public void keyTyped(KeyEvent e) { }
					public void keyPressed(KeyEvent e) { }
		
					public void keyReleased(KeyEvent e)
					{
						if (e.getKeyCode() == KeyEvent.VK_ENTER)
							handleSpectate();
					}
				});
		
		lstInGame.addListSelectionListener(new ListSelectionListener()
				{
					public void valueChanged(ListSelectionEvent e)
					{
						txtName.setText((String)lstInGame.getSelectedValue());
					}
				});
		
		pane.addHierarchyBoundsListener(new HierarchyBoundsListener()
				{
					public void ancestorMoved(HierarchyEvent e) { }
		
					public void ancestorResized(HierarchyEvent e)
					{
						Dimension d = f.getSize();
						width = d.width;
						height = d.height;
						doLayout();
					}
				});

		f.addWindowListener(new WindowListener()
				{
					public void windowOpened(WindowEvent e) { }
					public void windowClosing(WindowEvent e) { }
					public void windowIconified(WindowEvent e) { }
					public void windowDeiconified(WindowEvent e) { }
					public void windowActivated(WindowEvent e) { }
					public void windowDeactivated(WindowEvent e) { }

					public void windowClosed(WindowEvent e)
					{
						client.close();
					}
				});
		
		// Window settings
		f.setSize(width, height);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}
	
	public static void setupClient()
	{
		// Read in the config
		File conf = new File("config.txt");
		params = new HashMap<String, String>();
		
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

					// Handle notepad saving extra bytes for UTF8
					if (parts[0].charAt(0) == 65279)
						parts[0] = parts[0].substring(1);
					
					params.put(parts[0].trim(), parts[1].trim());
				}
				in.close();
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(
						f,
						"Encountered an error when parsing config.txt",
					    "Error",
					    JOptionPane.ERROR_MESSAGE);
			}
		}

		// Get missing information
		boolean newinfo = false;
		if (!params.containsKey("lollocation"))
		{
			String res = (String)JOptionPane.showInputDialog(
					f,
                    "Enter your LoL installation location\nE.g. C:\\Riot Games\\League of Legends\\",
                    "Login Information",
                    JOptionPane.QUESTION_MESSAGE);
			
			params.put("lollocation", res);
			newinfo = true;
		}

		if (!params.containsKey("region"))
		{
			String res = (String)JOptionPane.showInputDialog(
					f,
                    "Enter the region (NA/EUW/EUN)",
                    "Login Information",
                    JOptionPane.QUESTION_MESSAGE);
			
			params.put("region", res);
			newinfo = true;
		}

		if (!params.containsKey("version"))
		{
			String res = (String)JOptionPane.showInputDialog(
					f,
                    "Enter the Client Version for " + params.get("region") + "\nClient version can be found at the top left of the real client",
                    "Login Information",
                    JOptionPane.QUESTION_MESSAGE);

			params.put("version", res);
			newinfo = true;
		}

		if (!params.containsKey("user"))
		{
			String res = (String)JOptionPane.showInputDialog(
					f,
                    "Enter your login name for " + params.get("region"),
                    "Login Information",
                    JOptionPane.QUESTION_MESSAGE);

			params.put("user", res);
			newinfo = true;
		}

		if (!params.containsKey("pass"))
		{
			String res = (String)JOptionPane.showInputDialog(
					f,
                    "Enter the password for '" + params.get("user") + "'",
                    "Login Information",
                    JOptionPane.QUESTION_MESSAGE);

			params.put("pass", res);
		}
		
		// Set up config.txt if needed
		if (newinfo)
		{
			try
			{
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conf), "UTF-8"));
				out.write("lollocation=" + params.get("lollocation") + "\r\n");
				out.write("user=" + params.get("user") + "\r\n");
				//out.write("pass=" + params.get("pass") + "\r\n"); // Don't save password by default
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
			params.put("region2", "NA1");
		else if (region.equals("EUW"))
			params.put("region2", "EUW1");
		else if (region.equals("EUN"))
			params.put("region2", "EUN1");
		
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
			JOptionPane.showMessageDialog(
					f,
					"The installation location does not appear to be valid, make sure it is correct in config.txt.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
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
		params.put("maxGame", "" + maxGame);
		
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
		params.put("maxClient", "" + maxClient);
		
		// Connect
		client = new LoLRTMPSClient(params.get("region"), params.get("version"), params.get("user"), params.get("pass"));
		client.reconnect();
		
		// Enable the buttons
		btnName.setText("Spectate!");
		btnName.setEnabled(true);
		btnFile.setEnabled(true);
	}

	public static void doLayout()
	{
		lblName.setBounds(5, 5, 60, 25);
		txtName.setBounds(65, 5, width - 185, 25);
		btnName.setBounds(width - 120, 5, 107, 24);
		
		sep.setBounds(5, 35, width - 18, 5);
		
		lblFile.setBounds(5, 42, 60, 25);
		txtFile.setBounds(65, 42, width - 185, 25);
		btnFile.setBounds(width - 120, 42, 107, 24);
		
		lstScroll.setBounds(5, 72, width - 17, height - 104);
	}
	
	public static synchronized void handleSpectate()
	{
		String toSpec = txtName.getText();
		if (toSpec.length() == 0)
			return;
		
		try
		{
			// Get spectator info
			int id = client.invoke("gameService", "retrieveInProgressSpectatorGameInfo", new Object[] { toSpec });
			TypedObject result = client.getResult(id);
			TypedObject data = result.getTO("data");

			// Handle errors
			if (result.get("result").equals("_error"))
			{
				if (data.getTO("rootCause").type.equals("com.riotgames.platform.messaging.UnexpectedServiceException"))
				{
					JOptionPane.showMessageDialog(
							f,
							"No summoner found for " + toSpec + ".",
						    "Error",
						    JOptionPane.ERROR_MESSAGE);
				}
				else if (data.getTO("rootCause").type.equals("com.riotgames.platform.game.GameNotFoundException"))
				{
					JOptionPane.showMessageDialog(
							f,
							toSpec + " is not currently in a game.",
						    "Error",
						    JOptionPane.ERROR_MESSAGE);
				}
				else
				{
					JOptionPane.showMessageDialog(
							f,
							"Encountered an error when retrieving spectator information for " + toSpec + ":\n" + data.getTO("rootCause").get("localizedMessage"),
						    "Error",
						    JOptionPane.ERROR_MESSAGE);
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
				
				// Set up the command line
				String loc = params.get("lollocation");
				File dir = new File(loc + "RADS\\solutions\\lol_game_client_sln\\releases\\0.0.0." + params.get("maxGame") + "\\deploy\\");
				String[] cmd = new String[] {
						loc + "RADS\\solutions\\lol_game_client_sln\\releases\\0.0.0." + params.get("maxGame") + "\\deploy\\League of Legends.exe",
						"8394",
						"LoLLauncher.exe",
						loc + "RADS\\projects\\lol_air_client\\releases\\0.0.0." + params.get("maxGame") + "\\deploy\\LolClient.exe",
						"spectator " + ip + ":" + port + " " + key + " " + gameID + " " + params.get("region2")
					};
				
				// Run (and make sure to consume output)
				Process game = Runtime.getRuntime().exec(cmd, null, dir);
				new StreamGobbler(game.getInputStream());
				new StreamGobbler(game.getErrorStream());
			}
		}
		catch (IOException e)
		{
			System.err.println("Encountered an error when trying to retrieve spectate information for " + toSpec + ":");
			e.printStackTrace();
		}
	}
	
	public static void setFile()
	{
		// Change focus first so no infinite loop
		btnFile.requestFocusInWindow();
		
		int returnVal = fc.showOpenDialog(f);
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return;
		
		File f = fc.getSelectedFile();

		txtFile.setText(f.getAbsolutePath());
	}
	
	public static synchronized void loadFile()
	{
		if (!client.isLoggedIn())
			return;
		
		String filename = txtFile.getText();
		if (filename == null)
			return;
		
		File file = new File(filename);
		if (!file.exists() || !file.isFile())
			return;

		// Clear old list
		lstModel.clear();
		
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			while ((line = in.readLine()) != null)
			{
				line = line.trim();
				line = line.replace("\"", "");
				
				// Handle notepad saving extra bytes for UTF8
				if (line.charAt(0) == 65279)
					line = line.substring(1);
				
				if (line.length() < 4)
					continue;
				
				// Invoke asynchronously
				final String name = line;
				client.invokeWithCallback("gameService", "retrieveInProgressSpectatorGameInfo", new Object[] { name },
						new Callback()
						{
							public void callback(TypedObject result)
							{
								if (result.get("result").equals("_result"))
									lstModel.addElement(name);
							}
						});
			}
			in.close();
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(
					f,
					"Error reading from " + filename + ":\n" + e.getMessage(),
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
		}
		
		// Wait for all requests to finish;
		client.join();
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
