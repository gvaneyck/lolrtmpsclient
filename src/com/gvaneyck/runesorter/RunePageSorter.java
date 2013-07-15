package com.gvaneyck.runesorter;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gvaneyck.rtmp.LoLRTMPSClient;
import com.gvaneyck.rtmp.encoding.TypedObject;
import com.gvaneyck.util.ConsoleWindow;

public class RunePageSorter {
    
    public static SorterWindow sorterWindow;
    public static ConsoleWindow consoleWindow;
    public static SettingsWindow settingsWindow;

    public static LoLRTMPSClient client;

    public static List<RunePage> runePages;
    public static List<MasteryPage> masteryPages = new ArrayList<MasteryPage>();
    public static Map<Integer, Rune> runes = new HashMap<Integer, Rune>();

    public static int acctId = 0;
    public static int summId = 0;
    public static int summLevel = 0;

    public static void main(String[] args) {
        sorterWindow = new SorterWindow();
        consoleWindow = new ConsoleWindow(SorterWindow.width, 0);
        settingsWindow = new SettingsWindow("config.txt");
        
        sorterWindow.addWindowFocusListener(new WindowFocusListener() {
            public void windowLostFocus(WindowEvent e) { }
            
            public void windowGainedFocus(WindowEvent e) {
                if (settingsWindow.isVisible()) {
                    settingsWindow.toFront();
                    settingsWindow.requestFocus();
                    //settingsWindow.repaint();
                }
            }
        });
        
        setupClient();
    }

    public static void setupClient() {
        settingsWindow.setVisible(true);
        
        while (settingsWindow.isVisible()) {
            try { Thread.sleep(10); } catch (Exception e) { }
        }
        
        // Connect
        Settings settings = settingsWindow.getSettings();
        client = new LoLRTMPSClient(settings.region, settings.version, settings.username, settings.password);
        
        try {
            client.connectAndLogin();
            initPages();
        }
        catch (IOException e) {
            System.out.println("Error connecting to server: ");
            e.printStackTrace();
            
            Thread t = new Thread() {
                public void run() {
                    setupClient();
                }
            };
            t.start();
        }
    }

    public static void initPages() {
        try {
            // Get the account and summoner ID
            int id = client.invoke("clientFacadeService", "getLoginDataPacketForUser", new Object[] {});
            TypedObject result = client.getResult(id);
            TypedObject data = result.getTO("data").getTO("body").getTO("allSummonerData");
            TypedObject summoner = data.getTO("summoner");
            acctId = summoner.getInt("acctId");
            summId = summoner.getInt("sumId");

            // Get the summoner level
            TypedObject summonerLevel = data.getTO("summonerLevelAndPoints");
            summLevel = summonerLevel.getInt("summonerLevel");
            
            // Get our rune inventory
            id = client.invoke("summonerRuneService", "getSummonerRuneInventory", new Object[] { summId });
            result = client.getResult(id);
            Object[] runeList = result.getTO("data").getTO("body").getArray("summonerRunes");
            for (Object rune : runeList) {
                Rune r = new Rune((TypedObject)rune);
                runes.put(r.id, r);
            }

            // Get our rune pages
            runePages = getRunePages(summId);
            Collections.sort(runePages);
            sorterWindow.updateRunePages(runePages);

            // Get our mastery pages
            id = client.invoke("summonerService", "getAllSummonerDataByAccount", new Object[] { acctId });
            Object[] masteryBookPages = client.getResult(id).getTO("data").getTO("body").getTO("masteryBook").getArray("bookPages");
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
    
    public static List<RunePage> getRunePages(String summoner) throws IOException {
        int id = client.invoke("summonerService", "getSummonerByName", new Object[] { summoner });
        return getRunePages(client.getResult(id).getTO("data").getTO("body").getInt("summonerId"));
    }
    
    public static List<RunePage> getRunePages(int summonerId) throws IOException {
        int id = client.invoke("spellBookService", "getSpellBook", new Object[] { summonerId });
        Object[] runeBookPages = client.getResult(id).getTO("data").getTO("body").getArray("bookPages");
        
        List<RunePage> result = new ArrayList<RunePage>();
        for (Object o : runeBookPages)
            result.add(new RunePage((TypedObject)o));
        
        return result;
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
