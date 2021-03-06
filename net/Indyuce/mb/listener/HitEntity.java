package net.Indyuce.mb.listener;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import net.Indyuce.mb.api.MoarBow;

public class HitEntity implements Listener {
	@EventHandler
	public void a(EntityDamageByEntityEvent e) {
		if (e.getDamager().getType() != EntityType.ARROW)
			return;

		Arrow a = (Arrow) e.getDamager();
		MoarBow b = ShootBow.type.get(a.getUniqueId());
		if (b == null || !(a.getShooter() instanceof Player))
			return;

		b.getBowClass().hit(e, a, e.getEntity(), (Player) a.getShooter());
	}
}
