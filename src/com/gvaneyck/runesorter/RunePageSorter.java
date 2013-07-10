package com.gvaneyck.runesorter;

import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import com.gvaneyck.rtmp.LoLRTMPSClient;
import com.gvaneyck.rtmp.ServerInfo;
import com.gvaneyck.rtmp.encoding.TypedObject;
import com.gvaneyck.util.ConsoleWindow;

public class RunePageSorter {
    
    public static SorterWindow sorterWindow;

    public static LoLRTMPSClient client;
    public static Map<String, String> params;

    public static Map<String, ServerInfo> regionMap;

    public static List<RunePage> runePages = new ArrayList<RunePage>();
    public static List<MasteryPage> masteryPages = new ArrayList<MasteryPage>();

    public static int acctId = 0;
    public static int summId = 0;

    static {
        regionMap = new HashMap<String, ServerInfo>();
        regionMap.put("NORTH AMERICA", ServerInfo.NA);
        regionMap.put("EUROPE WEST", ServerInfo.EUW);
        regionMap.put("EUROPE NORDIC & EAST", ServerInfo.EUNE);
        regionMap.put("KOREA", ServerInfo.KR);
        regionMap.put("BRAZIL", ServerInfo.BR);
        regionMap.put("TURKEY", ServerInfo.TR);
        regionMap.put("RUSSIA", ServerInfo.RU);
        regionMap.put("LATIN AMERICA NORTH", ServerInfo.LAN);
        regionMap.put("LATIN AMERICA SOUTH", ServerInfo.LAS);
        regionMap.put("OCEANIA", ServerInfo.OCE);
        regionMap.put("PUBLIC BETA ENVIRONMENT", ServerInfo.PBE);
        regionMap.put("SINGAPORE/MALAYSIA", ServerInfo.SG);
        regionMap.put("TAIWAN", ServerInfo.TW);
        regionMap.put("THAILAND", ServerInfo.TH);
        regionMap.put("PHILLIPINES", ServerInfo.PH);
        regionMap.put("VIETNAM", ServerInfo.VN);
    }

    public static void main(String[] args) {
        sorterWindow = new SorterWindow();
        setupClient();
        new ConsoleWindow(SorterWindow.width, 0);
    }

    public static void setupClient() {
        // Read in the config
        File conf = new File("config.txt");
        params = new HashMap<String, String>();

        // Parse if exists
        if (conf.exists()) {
            try {
                String line;
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(conf), "UTF-8"));
                while ((line = in.readLine()) != null) {
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
            catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Encountered an error when parsing config.txt", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Get missing information
        boolean newinfo = false;

        if (!params.containsKey("region") || !regionMap.containsKey(params.get("region").toUpperCase())) {
            String res = (String)JOptionPane.showInputDialog(
                    null,
                    "Select a region",
                    "Login Information",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[] {
                            "North America",
                            "Europe West",
                            "Europe Nordic & East",
                            "Korea",
                            "Brazil",
                            "Turkey",
                            "Russia",
                            "Latin America North",
                            "Latin America South",
                            "Oceania",
                            "Public Beta Environment",
                            "Singapore/Malaysia",
                            "Taiwan",
                            "Thailand",
                            "Phillipines",
                            "Vietnam" },
                    "North America");

            params.put("region", res);
            newinfo = true;
        }

        if (!params.containsKey("version")) {
            String res = (String)JOptionPane.showInputDialog(
                    null,
                    "Enter the Client Version for " + params.get("region") + "\nClient version can be found at the top left of the real client",
                    "Login Information",
                    JOptionPane.QUESTION_MESSAGE);

            params.put("version", res);
            newinfo = true;
        }

        if (!params.containsKey("user")) {
            String res = (String)JOptionPane.showInputDialog(
                    null,
                    "Enter your login name for " + params.get("region"),
                    "Login Information",
                    JOptionPane.QUESTION_MESSAGE);

            params.put("user", res);
            newinfo = true;
        }

        if (!params.containsKey("pass")) {
            JPanel panel = new JPanel();
            JLabel label = new JLabel("Enter the password for '" + params.get("user") + "'");
            JPasswordField pass = new JPasswordField(10);

            panel.setLayout(new GridLayout(0, 1));
            panel.add(label);
            panel.add(pass);

            JOptionPane.showOptionDialog(
                    null,
                    panel,
                    "Login Information",
                    JOptionPane.NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[] { "OK", "Cancel" },
                    "OK");

            String res = new String(pass.getPassword());

            params.put("pass", res);
        }

        // Set up config.txt if needed
        if (newinfo) {
            try {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conf), "UTF-8"));
                out.write("user=" + params.get("user") + "\r\n");
                //out.write("pass=" + params.get("pass") + "\r\n"); // Don't save password by default
                out.write("version=" + params.get("version") + "\r\n");
                out.write("region=" + params.get("region") + "\r\n");
                out.close();
            }
            catch (IOException e) {
                System.out.println("Encountered an error when creating config.txt:");
                e.printStackTrace();
            }
        }

        // Connect
        ServerInfo serverInfo = regionMap.get(params.get("region").toUpperCase());
        client = new LoLRTMPSClient(serverInfo, params.get("version"), params.get("user"), params.get("pass"));
        client.setLocale(params.get("locale"));
        client.reconnect();

        initPages();
    }

    public static void initPages() {
        try {
            // Get the account ID
            int id = client.invoke("clientFacadeService", "getLoginDataPacketForUser", new Object[] {});
            TypedObject summoner = client.getResult(id).getTO("data").getTO("body").getTO("allSummonerData").getTO("summoner");
            acctId = summoner.getInt("acctId");
            summId = summoner.getInt("sumId");

            // Get our pages
            id = client.invoke("summonerService", "getAllSummonerDataByAccount", new Object[] { acctId });
            TypedObject body = client.getResult(id).getTO("data").getTO("body");

            Object[] runeBookPages = body.getTO("spellBook").getArray("bookPages");
            for (Object o : runeBookPages)
                runePages.add(new RunePage((TypedObject)o));
            Collections.sort(runePages);
            sorterWindow.updateRunePages(runePages);

            Object[] masteryBookPages = body.getTO("masteryBook").getArray("bookPages");
            for (Object o : masteryBookPages)
                masteryPages.add(new MasteryPage((TypedObject)o));
            Collections.sort(masteryPages);
            sorterWindow.updateMasteryPages(masteryPages);

            sorterWindow.enableButtons();
        }
        catch (IOException e) {
            client.close();

            System.out.println("Failed to get account information:");
            e.printStackTrace();
            System.out.println();
            System.out.println("Restart the program to try again.");
        }
    }
    
    public static void sortRunes() {
        List<Integer> pageIds = new ArrayList<Integer>();
        for (RunePage page : runePages)
            pageIds.add(page.pageId);

        Collections.sort(runePages, new Comparator<RunePage>() {
            public int compare(RunePage page1, RunePage page2) {
                return page1.name.compareTo(page2.name);
            }
        });

        for (int i = 0; i < runePages.size(); i++)
            runePages.get(i).pageId = pageIds.get(i);

        savePages();
    }
    
    public static void sortMasteries() {
        List<Integer> pageIds = new ArrayList<Integer>();
        for (MasteryPage page : masteryPages)
            pageIds.add(page.pageId);

        Collections.sort(masteryPages, new Comparator<MasteryPage>() {
            public int compare(MasteryPage page1, MasteryPage page2) {
                return page1.name.compareTo(page2.name);
            }
        });

        for (int i = 0; i < masteryPages.size(); i++)
            masteryPages.get(i).pageId = pageIds.get(i);

        saveMasteries();
    }
    
    public static void moveRunePageUp(int index) {
        RunePage current = runePages.get(index);
        current.swap(runePages.get(index - 1));
        savePages();
    }
    
    public static void moveRunePageDown(int index) {
        RunePage current = runePages.get(index);
        current.swap(runePages.get(index + 1));
        savePages();
    }
    
    public static void moveMasteryPageUp(int index) {
        MasteryPage current = masteryPages.get(index);
        current.swap(masteryPages.get(index - 1));
        saveMasteries();
    }
    
    public static void moveMasteryPageDown(int index) {
        MasteryPage current = masteryPages.get(index);
        current.swap(masteryPages.get(index + 1));
        saveMasteries();
    }

    public static void savePages() {
        try {
            TypedObject[] pages2 = new TypedObject[runePages.size()];
            TypedObject currentPage = null;
            for (int i = 0; i < runePages.size(); i++) {
                pages2[i] = runePages.get(i).getSavePage(summId);
                if (runePages.get(i).current)
                    currentPage = pages2[i];
            }

            TypedObject args = new TypedObject("com.riotgames.platform.summoner.spellbook.SpellBookDTO");
            args.put("bookPages", TypedObject.makeArrayCollection(pages2));

            if (currentPage != null)
                args.put("defaultPage", currentPage);

            TypedObject sort = new TypedObject();
            sort.put("unique", false);
            sort.put("compareFunction", null);
            TypedObject fields = new TypedObject();
            fields.put("caseInsensitive", false);
            fields.put("name", "pageId");
            fields.put("numeric", null);
            fields.put("compareFunction", null);
            fields.put("descending", false);
            sort.put("fields", new Object[] { fields });
            args.put("sortByPageId", sort);

            args.put("summonerId", summId);
            args.put("dateString", null);
            args.put("futureData", null);
            args.put("dataVersion", null);

            int id = client.invoke("spellBookService", "saveSpellBook", new Object[] { args });
            TypedObject result = client.getResult(id);
            if (result.get("result").equals("_error"))
                System.out.println("Error changing rune page order");

            sorterWindow.updateRunePages(runePages);
        }
        catch (IOException e) {
            client.close();

            System.out.println("Failed to update Rune Pages:");
            e.printStackTrace();
            System.out.println();
            System.out.println("Restart the program to try again.");
        }
    }

    public static void saveMasteries() {
        try {
            TypedObject[] masteries2 = new TypedObject[masteryPages.size()];
            for (int i = 0; i < masteryPages.size(); i++)
                masteries2[i] = masteryPages.get(i).getSavePage(summId);

            TypedObject args = new TypedObject("com.riotgames.platform.summoner.masterybook.MasteryBookDTO");
            args.put("bookPages", TypedObject.makeArrayCollection(masteries2));

            TypedObject sort = new TypedObject();
            sort.put("unique", false);
            sort.put("compareFunction", null);
            TypedObject fields = new TypedObject();
            fields.put("caseInsensitive", false);
            fields.put("name", "pageId");
            fields.put("numeric", null);
            fields.put("compareFunction", null);
            fields.put("descending", false);
            sort.put("fields", new Object[] { fields });
            args.put("sortByPageId", sort);

            args.put("summonerId", summId);
            args.put("dateString", null);
            args.put("futureData", null);
            args.put("dataVersion", null);

            int id = client.invoke("masteryBookService", "saveMasteryBook", new Object[] { args });
            TypedObject result = client.getResult(id);
            if (result.get("result").equals("_error"))
                System.out.println(result + "\nError changing mastery page order");

            sorterWindow.updateMasteryPages(masteryPages);
        }
        catch (IOException e) {
            client.close();

            System.out.println("Failed to update Mastery Pages:");
            e.printStackTrace();
            System.out.println();
            System.out.println("Restart the program to try again.");
        }
    }

    public static void exit() {
        client.close();
    }
}
