package net.Indyuce.mb.resource.bow;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.Indyuce.mb.Eff;
import net.Indyuce.mb.Main;
import net.Indyuce.mb.api.SpecialBow;
import net.Indyuce.mb.util.VersionUtils;

public class CorrosiveBow implements SpecialBow {
	@Override
	public boolean shoot(EntityShootBowEvent e, Arrow a, Player p, ItemStack i) {
		return true;
	}

	@Override
	public void hit(EntityDamageByEntityEvent e, Arrow a, Entity p, Player t) {
		explode(a);
	}

	@Override
	public void land(Arrow a) {
		explode(a);
	}

	private static void explode(Arrow a) {
		int duration = Main.bows.getInt("CORROSIVE_BOW.duration") * 20;
		a.remove();
		Eff.SLIME.display(2, 2, 2, 0, 48, a.getLocation(), 200);
		Eff.VILLAGER_HAPPY.display(2, 2, 2, 0, 32, a.getLocation(), 200);
		VersionUtils.sound(a.getLocation(), "BLOCK_BREWING_STAND_BREW", 3, 0);
		for (Entity ent : a.getNearbyEntities(5, 5, 5))
			if (ent instanceof LivingEntity) {
				((LivingEntity) ent).removePotionEffect(PotionEffectType.POISON);
				((LivingEntity) ent).addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration, 1));
			}
	}
}
