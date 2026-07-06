package dev.riftwatch.ssmcooldownhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks kit ability cooldowns from chat, not from the server's transient
 * actionbar/boss-bar widget. When a "You used <ability>." message is seen,
 * we start a fixed-length, client-side timer for that ability (duration
 * looked up from ABILITY_COOLDOWNS, since the chat line itself doesn't
 * carry a duration). The sidebar always shows every known ability, counting
 * down or "Ready", regardless of what's in your hand.
 */
public class SSMCooldownHudClient implements ClientModInitializer {

	public static final String MOD_ID = "ssm-cooldown-hud";

	private static final CooldownTracker TRACKER = new CooldownTracker();

	// Matches "... You used <ability>." anywhere in the message (handles a
	// leading "Game> " prefix or similar) and captures the ability name.
	private static final Pattern USED_ABILITY_PATTERN =
			Pattern.compile("You used ([\\p{L}0-9 ]+?)\\.?\\s*$");

	// --- Fill this in with your kit's real abilities and cooldowns. ---
	// The key must match exactly what appears after "You used " in chat.
	// Durations are in seconds. Add/remove/adjust as needed.
	private static final Map<String, Double> ABILITY_COOLDOWNS = new LinkedHashMap<>();
	static {
		ABILITY_COOLDOWNS.put("Super Squid", 9.0);
		ABILITY_COOLDOWNS.put("Ink Shotgun", 7.0);   // confirm real value
		ABILITY_COOLDOWNS.put("Fish Flurry", 16.0);
		// ABILITY_COOLDOWNS.put("Deadly Bones", 5.0);
		// ABILITY_COOLDOWNS.put("Chicken Missile", 6.0);
	}

	@Override
	public void onInitializeClient() {
		// Pre-register every known ability so the sidebar shows the full
		// kit (as "Ready") from the moment you spawn.
		for (String ability : ABILITY_COOLDOWNS.keySet()) {
			TRACKER.registerAbility(ability);
		}

		ClientReceiveMessageEvents.GAME.register(this::onGameMessage);

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "cooldown_sidebar"),
				this::renderSidebar
		);
	}

	private void onGameMessage(Component message, boolean overlay) {
		String plain = message.getString().trim();
		if (plain.isBlank()) {
			return;
		}

		Matcher matcher = USED_ABILITY_PATTERN.matcher(plain);
		if (!matcher.find()) {
			return;
		}

		String abilityName = matcher.group(1).trim();
		Double duration = ABILITY_COOLDOWNS.get(abilityName);
		if (duration == null) {
			// Unmapped ability - log it so you know to add it above.
			System.out.println("[SSM-HUD] Unmapped ability \"" + abilityName
					+ "\" - add it to ABILITY_COOLDOWNS.");
			return;
		}

		TRACKER.startCooldown(abilityName, duration);
	}

	// --- HUD layout - tweak these to reposition / resize the sidebar. ---
	// SCREEN_X / SCREEN_Y are the on-screen pixel position of the top-left
	// corner of the box (0,0 is the top-left of the game window).
	private static final int SCREEN_X = 4;
	private static final int SCREEN_Y = 250;
	// 1.0 = normal Minecraft font size, 2.0 = double size, etc.
	private static final float TEXT_SCALE = 1.0f;
	// Padding (in local/unscaled units) between the text and the box edge.
	private static final int PADDING = 3;
	private static final int LINE_HEIGHT = 11;
	private static final int READY_COLOR = 0xFF55FF55;
	private static final int COOLDOWN_COLOR = 0xFFFFFFFF;
	// ARGB - 0xA0 alpha (~63%) black, i.e. a translucent backing panel.
	private static final int BACKGROUND_COLOR = 0xA0000000;

	private void renderSidebar(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		Font font = Minecraft.getInstance().font;

		// Work out the widest line so the background box fits everything.
		int maxTextWidth = 0;
		int lineCount = 0;
		for (CooldownTracker.Entry entry : TRACKER.allEntries()) {
			String line = entry.name() + ": " + labelFor(entry);
			maxTextWidth = Math.max(maxTextWidth, font.width(line));
			lineCount++;
		}
		if (lineCount == 0) {
			return;
		}

		// All drawing below happens inside a scaled coordinate space, so we
		// convert the desired screen position into local (pre-scale) units.
		// Everything drawn at local (0,0) after this ends up at
		// (SCREEN_X, SCREEN_Y) on screen once the scale is applied.
		float localX = SCREEN_X / TEXT_SCALE;
		float localY = SCREEN_Y / TEXT_SCALE;

		int boxWidth = maxTextWidth + PADDING * 2;
		int boxHeight = lineCount * LINE_HEIGHT + PADDING * 2;

		graphics.pose().pushMatrix();
		graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);

		int x = Math.round(localX);
		int y = Math.round(localY);

		graphics.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND_COLOR);

		int textX = x + PADDING;
		int textY = y + PADDING;
		for (CooldownTracker.Entry entry : TRACKER.allEntries()) {
			boolean ready = entry.isReady();
			int color = ready ? READY_COLOR : COOLDOWN_COLOR;
			graphics.text(font, entry.name() + ": " + labelFor(entry), textX, textY, color, true);
			textY += LINE_HEIGHT;
		}

		graphics.pose().popMatrix();
	}

	private String labelFor(CooldownTracker.Entry entry) {
		return entry.isReady() ? "Ready" : String.format("%.1fs", entry.remainingSeconds());
	}
}