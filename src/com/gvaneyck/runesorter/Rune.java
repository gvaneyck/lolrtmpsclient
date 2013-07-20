package com.gvaneyck.runesorter;

import com.gvaneyck.rtmp.encoding.TypedObject;

public class Rune {
    public int id;
    public String name;
    public int tier;
    public int quantity;
    public double effect;
    public String effectName;
    
    public Rune(TypedObject rune) {
        id = rune.getInt("runeId");
        name = rune.getTO("rune").getString("name");
        tier = rune.getTO("rune").getInt("tier");
    
        if (rune.containsKey("quantity"))
            quantity = rune.getInt("quantity");
        else
            quantity = 1;

        TypedObject effectInfo = (TypedObject)rune.getTO("rune").getArray("itemEffects")[0];
        effect = Double.parseDouble(effectInfo.getString("value"));
        effectName = effectInfo.getTO("effect").getString("name");
    }
    
    public String toString() {
        return String.format("%s: +%.2f %s", name, effect, effectName);
    }
}
