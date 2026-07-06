package dev.riftwatch.ssmcooldownhud;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds cooldown state for every kit ability, keyed by ability name.
 * Entries are never removed once created - once an ability has been
 * registered or used, it stays in the sidebar permanently, flipping
 * between "counting down" and "ready".
 */
public class CooldownTracker {

	public record Entry(String name, long startedAtMillis, long durationMillis) {
		public double remainingSeconds() {
			long elapsed = System.currentTimeMillis() - startedAtMillis;
			long remainingMillis = Math.max(0, durationMillis - elapsed);
			return remainingMillis / 1000.0;
		}

		public boolean isReady() {
			return remainingSeconds() <= 0.0;
		}
	}

	// LinkedHashMap so entries render in first-registered order.
	private static final Map<String, Double> ABILITY_COOLDOWNS = new LinkedHashMap<>();
	static {
		// --- Sky Squid ---
		ABILITY_COOLDOWNS.put("Ink Shotgun", 6.0);   	// TODO: confirm real cooldown
		ABILITY_COOLDOWNS.put("Super Squid", 9.0);	// TODO: short cooldown per wiki - confirm
		ABILITY_COOLDOWNS.put("Fish Flurry", 16.0);

		// --- Chicken ---
		ABILITY_COOLDOWNS.put("Egg Blaster", 0.0);
		ABILITY_COOLDOWNS.put("Chicken Missile", 0.0); // resets on hit - see note below

		// --- Skeleton ---
		ABILITY_COOLDOWNS.put("Bone Explosion", 0.0);
		ABILITY_COOLDOWNS.put("Roped Arrow", 0.0);

		// --- Spider ---
		ABILITY_COOLDOWNS.put("Spider Leap", 0.0);
		ABILITY_COOLDOWNS.put("Spin Web", 0.0);
		ABILITY_COOLDOWNS.put("Needler", 0.0);

		// --- Iron Golem ---
		ABILITY_COOLDOWNS.put("Iron Hook", 0.0);

		// --- Guardian ---
		ABILITY_COOLDOWNS.put("Target Laser", 0.0);
		ABILITY_COOLDOWNS.put("Water Splash", 0.0);

		// --- Magma Cube ---
		ABILITY_COOLDOWNS.put("Flame Dash", 0.0);
		// Fireball is thrown, not sure if it fires a "You used" message - confirm in log

		// --- Cow ---
		ABILITY_COOLDOWNS.put("Angry Herd", 0.0);
		ABILITY_COOLDOWNS.put("Milk Spiral", 0.0);

		// --- Witch ---
		ABILITY_COOLDOWNS.put("Daze Potions", 0.0);
		ABILITY_COOLDOWNS.put("Bat Waves", 0.0);

		// --- Wither Skeleton ---
		ABILITY_COOLDOWNS.put("Wither Skull", 0.0);
		ABILITY_COOLDOWNS.put("Wither Image", 0.0);
	}

	// Live per-ability state (start time + duration), keyed by ability name.
	// LinkedHashMap so entries render in first-registered order.
	private final Map<String, Entry> abilities = new LinkedHashMap<>();

	/**
	 * Adds an ability to the sidebar in the "Ready" state, if it isn't
	 * already tracked. Call this once at startup for every ability you
	 * know about, so the sidebar shows the full kit immediately.
	 */
	public synchronized void registerAbility(String abilityName) {
		abilities.putIfAbsent(abilityName, new Entry(abilityName, 0L, 0L));
	}

	/**
	 * Call this whenever we see a "You used <ability>" chat message.
	 * durationSeconds is the ability's FULL, fixed cooldown length -
	 * looked up from ABILITY_COOLDOWNS, not parsed from the message.
	 */
	public synchronized void startCooldown(String abilityName, double durationSeconds) {
		abilities.put(abilityName, new Entry(abilityName, System.currentTimeMillis(), (long) (durationSeconds * 1000)));
	}

	public synchronized Iterable<Entry> allEntries() {
		return abilities.values();
	}
}