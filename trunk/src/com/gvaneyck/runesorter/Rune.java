package com.gvaneyck.runesorter;

import java.util.HashMap;
import java.util.Map;

import com.gvaneyck.rtmp.encoding.TypedObject;

public class Rune {
    public int id;
    public String name;
    public int tier;
    public int quantity;
    public double effect;
    public String effectName;
    
    public static final Map<String, String> effectNames;
    
    static {
        effectNames = new HashMap<String, String>();
        effectNames.put("FlatArmorMod", "Armor");
        effectNames.put("FlatCritChanceMod", "Crit Chance");
        effectNames.put("FlatCritDamageMod", "Crit Damage");
        effectNames.put("FlatEnergyPoolMod", "Energy");
        effectNames.put("FlatEnergyRegenMod", "Energy/5");
        effectNames.put("FlatHPPoolMod", "HP");
        effectNames.put("FlatHPRegenMod", "HP/5");
        effectNames.put("FlatMagicDamageMod", "AP");
        effectNames.put("FlatMPPoolMod", "MP");
        effectNames.put("FlatMPRegenMod", "MP/5");
        effectNames.put("FlatPhysicalDamageMod", "Damage");
        effectNames.put("FlatSpellBlockMod", "MR");
        effectNames.put("PercentAttackSpeedMod", "AS");
        effectNames.put("PercentEXPBonus", "XP");
        effectNames.put("PercentHPPoolMod", "HP");
        effectNames.put("PercentLifeStealMod", "Life Steal");
        effectNames.put("PercentMovementSpeedMod", "MS");
        effectNames.put("PercentSpellVampMod", "Spell Vamp");
        effectNames.put("rFlatArmorModPerLevel", "Armor");
        effectNames.put("rFlatArmorPenetrationMod", "Armor Pen");
        effectNames.put("rFlatEnergyModPerLevel", "Energy");
        effectNames.put("rFlatEnergyRegenModPerLevel", "Energy/5");
        effectNames.put("rFlatGoldPer10Mod", "Gold/10");
        effectNames.put("rFlatHPModPerLevel", "HP");
        effectNames.put("rFlatHPRegenModPerLevel", "HP/5");
        effectNames.put("rFlatMagicDamageModPerLevel", "AP");
        effectNames.put("rFlatMagicPenetrationMod", "Magic Pen");
        effectNames.put("rFlatMPModPerLevel", "MP");
        effectNames.put("rFlatMPRegenModPerLevel", "MP/5"); //PP 8.9
        effectNames.put("rFlatPhysicalDamageModPerLevel", "Damage");
        effectNames.put("rFlatSpellBlockModPerLevel", "MR");
        effectNames.put("rPercentCooldownMod", "Cooldown");
        effectNames.put("rPercentCooldownModPerLevel", "Cooldown");
        effectNames.put("rPercentTimeDeadMod", "Time Dead");

    }
    
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
    
    public Rune(Rune r1, Rune r2) {
        quantity = 1;
        effect = r1.quantity * r1.effect + r2.quantity * r2.effect;
        effectName = r1.effectName;
    }
    
    public String toString() {
        String value;
        if (effectName.contains("Percent"))
            value = String.format("%+.2f%%", effect * quantity * 100);
        else
            value = String.format("%+.2f", effect * quantity);
        
        return String.format("%s: %s %s", name, value, effectName);
    }
    
    public static String translateEffect(String effect) {
        return effectNames.get(effect);
    }
}
