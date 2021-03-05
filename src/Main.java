import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener{
	
	public static final String MS = "��f[ ��b���� ��f] "; 
	public static final String LORE_POST = "��7���� �ӵ� 1 / ��b"; //���� �ӵ� �Ľ̿� ���ڿ�
	
	private static AttributeModifier attackCooldownModi= new AttributeModifier("BetterPvP", 99999999.0D, AttributeModifier.Operation.ADD_NUMBER);
	private static int attackKey = 16;
	
	public HashMap<String, Long> attackDelayMap = new HashMap<String, Long>();
	
	public void onEnable() {
	
		getServer().getLogger().info("���� �ӵ� ���� �ñ� Ȱ��ȭ");
		getServer().getPluginManager().registerEvents(this, this); //�̺�Ʈ ���
	}
	
	public void onDisable() {
		getServer().getLogger().info("���� �ӵ� ���� �ñ� ��Ȱ��ȭ");
	}
	
	public boolean canEnchant(ItemStack item) {
		if(item == null) return false; //null �̸� return
		if(item.getType() == Material.AIR) return false; // ���� �������� return
		
		return true;
	}
	
	public boolean enchantAttackDelay(ItemStack item, int delay) { //���� ������ ����
		//if(item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			
			double calcDelay = (double)delay / 1000; //ms ������ s ������
			String delayString = calcDelay + "s"; 
			
			String addString = LORE_POST + delayString; //���߿� �Ľ��ϱ� ���ϰ� ���ڿ� �ٿ���
			
			boolean isChange = false;
			List<String> loreList = meta.getLore();
			if(loreList == null || loreList.size() == 0) { //lore ���ٸ� ���Ӱ� ����
				loreList = new ArrayList<String>();
				loreList.add("");
			} else {
				for(int i = 0; i < loreList.size(); i++) { //�̹� �ִٸ� ���Ӱ� ��ü
					String str = loreList.get(i);
					if(str.contains(LORE_POST)) {
						isChange = true;
						loreList.set(i, addString);
					}
				}
			}
			if(!isChange) loreList.add(addString); //loreList�� ������ �����ٸ� �߰��ϴ°ɷ�
			meta.setLore(loreList);
			item.setItemMeta(meta);
		//} 
		
		return false;
	}
	
	public String getDelayString(List<String> loreList) { //lore���� delay �� ���� ���ڿ� ��� 
		if(loreList != null) {
			for(String str : loreList) {
				if(str.contains(LORE_POST)) return str;
			}
		} 
		return null;
		
	}
	
	//���� ������ Ȱ��/��Ȱ��
	public static void attackDelay(Player p, boolean enable) {
		try {
			if(enable) {
				Collection<AttributeModifier> c = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getModifiers();
				//p.sendMessage(c.toString());
				for(AttributeModifier a : c) {
					p.getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(a);
				}			
			} else {
				p.getAttribute(Attribute.GENERIC_ATTACK_SPEED).addModifier(attackCooldownModi);
			}
		} catch(IllegalArgumentException e) {
			
		}
	}
	
	public long parseAttackDelay(String delayString) {//���ڿ����� delay �� ���
		if(delayString == null) return -1;
		//getServer().getLogger().info(delayString);
		delayString = delayString.replace(LORE_POST, "");
		//getServer().getLogger().info(delayString);
		delayString = delayString.substring(0, delayString.length() - 1);
		//getServer().getLogger().info(delayString);
		double delay = -1; 
		try {
			delay = Double.parseDouble(delayString);
			delay *= 1000;
		}catch (NumberFormatException e) {
			
		}
		return (long)delay;
	}
	
	public void updateDelayMap(Player p, long delay) {
		if(delay < 0) {
			return;
		}
		UUID uuid = p.getUniqueId();
		String id = uuid.toString();
		attackDelayMap.put(id, System.currentTimeMillis()+delay); //������ ���Ӱ� ������Ʈ
	}
	
	public boolean canAttack(Player p, boolean isAlarm) {
		UUID uuid = p.getUniqueId();
		String id = uuid.toString();
		if(attackDelayMap.containsKey(id)) {
			long nextAttackTime = attackDelayMap.get(id);
			if(System.currentTimeMillis() < nextAttackTime) {
				//getServer().getLogger().info(isAlarm+"");
				if(isAlarm) {
					long leftTime = nextAttackTime - System.currentTimeMillis();
					p.sendMessage(MS+((double)leftTime / 1000)+"�� �Ŀ� ���� �����մϴ�.");
				}
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}
	
	@EventHandler
	public void onCommandExecute(PlayerCommandPreprocessEvent e) {
		String[] args = e.getMessage().split(" ");
		String cmd = args[0];
		
		Player p = e.getPlayer();
		
		if(cmd.equalsIgnoreCase("/ad")) {
			if(args.length == 1) {
				p.sendMessage(MS+"/ad <���ݵ�����>");
				p.sendMessage(MS+"���� �������� ������ ms(�и�������)�Դϴ�.");
			} else {
				int delay = 0;
				
				String delayString = args[1];
				try {
					delay = Integer.parseInt(delayString);
				}catch (NumberFormatException exception1) {
					p.sendMessage(MS+"<���ݵ�����> �� ms ������ ������ �Է����ּ���.");
				}
				
				ItemStack item = p.getInventory().getItemInMainHand();
				if(canEnchant(item)) {
					enchantAttackDelay(item, delay);
				} else {
					p.sendMessage(MS+"�ش� �������� ���� �ӵ��� �ο��� �� �����ϴ�.");
				}
			}
		}
	}
	
	@EventHandler
	public void onEntityDamagedByEntity(EntityDamageByEntityEvent e) {
		
		Entity damager = e.getDamager();
		
		Entity victimEntity = e.getEntity(); //�⺻ �ʱ�ȭ
		LivingEntity victim = null;
		
		if(victimEntity instanceof LivingEntity) { //��ũ ��ü�� ���� �޴� ������ ������ �⺻���� (12ƽ)
			victim = (LivingEntity) victimEntity;
			victim.setMaximumNoDamageTicks(12);
			victim.setNoDamageTicks(12);
		} else { //�����ڰ� ��ƼƼ�� �ƴϸ� return
			return; 
		}
		
		if(damager instanceof Player) {
			
			Player attacker = (Player) damager;
			ItemStack attackWeapon = attacker.getInventory().getItemInMainHand();
			if(attackWeapon.hasItemMeta()) {
				if(canAttack(attacker, true)) {
									
					String delayString = getDelayString(attackWeapon.getItemMeta().getLore());
					
					if(delayString != null) { //�����̰� ����� ������
						victim.setMaximumNoDamageTicks(0); //���� ���� 0����
						victim.setNoDamageTicks(0);
						
						long delay = parseAttackDelay(delayString);
						updateDelayMap(attacker, delay);
					}			

				} else {
					e.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		attackDelay(p, false);
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		attackDelay(p, false);
	}

}


