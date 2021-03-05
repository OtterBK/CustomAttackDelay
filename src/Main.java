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
	
	public static final String MS = "§f[ §b공속 §f] "; 
	public static final String LORE_POST = "§7공격 속도 1 / §b"; //공격 속도 파싱용 문자열
	
	private static AttributeModifier attackCooldownModi= new AttributeModifier("BetterPvP", 99999999.0D, AttributeModifier.Operation.ADD_NUMBER);
	private static int attackKey = 16;
	
	public HashMap<String, Long> attackDelayMap = new HashMap<String, Long>();
	
	public void onEnable() {
	
		getServer().getLogger().info("공격 속도 제한 플긴 활성화");
		getServer().getPluginManager().registerEvents(this, this); //이벤트 등록
	}
	
	public void onDisable() {
		getServer().getLogger().info("공격 속도 제한 플긴 비활성화");
	}
	
	public boolean canEnchant(ItemStack item) {
		if(item == null) return false; //null 이면 return
		if(item.getType() == Material.AIR) return false; // 공기 아이템은 return
		
		return true;
	}
	
	public boolean enchantAttackDelay(ItemStack item, int delay) { //공격 딜레이 적용
		//if(item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			
			double calcDelay = (double)delay / 1000; //ms 단위를 s 단위로
			String delayString = calcDelay + "s"; 
			
			String addString = LORE_POST + delayString; //나중에 파싱하기 편하게 문자열 붙여줌
			
			boolean isChange = false;
			List<String> loreList = meta.getLore();
			if(loreList == null || loreList.size() == 0) { //lore 없다면 새롭게 생성
				loreList = new ArrayList<String>();
				loreList.add("");
			} else {
				for(int i = 0; i < loreList.size(); i++) { //이미 있다면 새롭게 대체
					String str = loreList.get(i);
					if(str.contains(LORE_POST)) {
						isChange = true;
						loreList.set(i, addString);
					}
				}
			}
			if(!isChange) loreList.add(addString); //loreList에 공속이 없었다면 추가하는걸로
			meta.setLore(loreList);
			item.setItemMeta(meta);
		//} 
		
		return false;
	}
	
	public String getDelayString(List<String> loreList) { //lore에서 delay 값 적힌 문자열 얻기 
		if(loreList != null) {
			for(String str : loreList) {
				if(str.contains(LORE_POST)) return str;
			}
		} 
		return null;
		
	}
	
	//어택 딜레이 활성/비활성
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
	
	public long parseAttackDelay(String delayString) {//문자열에서 delay 값 얻기
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
		attackDelayMap.put(id, System.currentTimeMillis()+delay); //딜레이 새롭게 업데이트
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
					p.sendMessage(MS+((double)leftTime / 1000)+"초 후에 공격 가능합니다.");
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
				p.sendMessage(MS+"/ad <공격딜레이>");
				p.sendMessage(MS+"공격 딜레이의 단위는 ms(밀리세컨드)입니다.");
			} else {
				int delay = 0;
				
				String delayString = args[1];
				try {
					delay = Integer.parseInt(delayString);
				}catch (NumberFormatException exception1) {
					p.sendMessage(MS+"<공격딜레이> 는 ms 단위의 정수만 입력해주세요.");
				}
				
				ItemStack item = p.getInventory().getItemInMainHand();
				if(canEnchant(item)) {
					enchantAttackDelay(item, delay);
				} else {
					p.sendMessage(MS+"해당 아이템은 공격 속도를 부여할 수 없습니다.");
				}
			}
		}
	}
	
	@EventHandler
	public void onEntityDamagedByEntity(EntityDamageByEntityEvent e) {
		
		Entity damager = e.getDamager();
		
		Entity victimEntity = e.getEntity(); //기본 초기화
		LivingEntity victim = null;
		
		if(victimEntity instanceof LivingEntity) { //마크 자체의 피해 받는 딜레이 간격을 기본으로 (12틱)
			victim = (LivingEntity) victimEntity;
			victim.setMaximumNoDamageTicks(12);
			victim.setNoDamageTicks(12);
		} else { //피해자가 엔티티가 아니면 return
			return; 
		}
		
		if(damager instanceof Player) {
			
			Player attacker = (Player) damager;
			ItemStack attackWeapon = attacker.getInventory().getItemInMainHand();
			if(attackWeapon.hasItemMeta()) {
				if(canAttack(attacker, true)) {
									
					String delayString = getDelayString(attackWeapon.getItemMeta().getLore());
					
					if(delayString != null) { //딜레이가 적용된 무기라면
						victim.setMaximumNoDamageTicks(0); //피해 간격 0으로
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


