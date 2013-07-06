package com.gvaneyck.rtmp;

/**
 * Class for storing connection information
 */
public class ServerInfo {
    public static final ServerInfo NA = new ServerInfo("NA", "NA1", "prod.na1.lol.riotgames.com", "https://lq.na1.lol.riotgames.com/");
    public static final ServerInfo EUW = new ServerInfo("EUW", "EUW1", "prod.eu.lol.riotgames.com", "https://lq.eu.lol.riotgames.com/");
    public static final ServerInfo EUNE = new ServerInfo("EUNE", "EUN1", "prod.eun1.lol.riotgames.com", "https://lq.eun1.lol.riotgames.com/");
    public static final ServerInfo KR = new ServerInfo("KR", "prod.kr.lol.riotgames.com", "https://lq.kr.lol.riotgames.com/");
    public static final ServerInfo BR = new ServerInfo("BR", "BR1", "prod.br.lol.riotgames.com", "https://lq.br.lol.riotgames.com/");
    public static final ServerInfo TR = new ServerInfo("TR", "TR1", "prod.tr.lol.riotgames.com", "https://lq.tr.lol.riotgames.com/");
    public static final ServerInfo RU = new ServerInfo("RU", "RU", "prod.ru.lol.riotgames.com", "https://lq.ru.lol.riotgames.com/");
    public static final ServerInfo LAN = new ServerInfo("LAN", "LA1", "prod.la1.lol.riotgames.com", "https://lq.la1.lol.riotgames.com/");
    public static final ServerInfo LAS = new ServerInfo("LAS", "LA2", "prod.la2.lol.riotgames.com", "https://lq.la2.lol.riotgames.com/");
    public static final ServerInfo OCE = new ServerInfo("OCE", "OC1", "prod.oc1.lol.riotgames.com", "https://lq.oc1.lol.riotgames.com/");
    public static final ServerInfo PBE = new ServerInfo("PBE", "PBE1", "prod.pbe1.lol.riotgames.com", "https://lq.pbe1.lol.riotgames.com/");
    public static final ServerInfo SG = new ServerInfo("SG", "prod.lol.garenanow.com", "https://lq.lol.garenanow.com/", true);
    public static final ServerInfo TW = new ServerInfo("TW", "prodtw.lol.garenanow.com", "https://loginqueuetw.lol.garenanow.com/", true);
    public static final ServerInfo TH = new ServerInfo("TH", "prodth.lol.garenanow.com", "https://lqth.lol.garenanow.com/", true);
    public static final ServerInfo PH = new ServerInfo("PH", "prodph.lol.garenanow.com", "https://storeph.lol.garenanow.com/", true);
    public static final ServerInfo VN = new ServerInfo("VN", "prodvn.lol.garenanow.com", "https://lqvn.lol.garenanow.com/", true);

    public String region;
    public String platform; // The internal platform name, used for spectating
    public String server;
    public String loginQueue;
    public boolean useGarena;

    public ServerInfo(String region, String server, String loginQueue) {
        this(region, region, server, loginQueue, false);
    }

    public ServerInfo(String region, String server, String loginQueue, boolean useGarena) {
        this(region, region, server, loginQueue, useGarena);
    }

    public ServerInfo(String region, String platform, String server, String loginQueue) {
        this(region, platform, server, loginQueue, false);
    }

    public ServerInfo(String region, String platform, String server, String loginQueue, boolean useGarena) {
        this.region = region;
        this.platform = platform;
        this.server = server;
        this.loginQueue = loginQueue;
        this.useGarena = useGarena;
    }

    public String toString() {
        return region;
    }
}
