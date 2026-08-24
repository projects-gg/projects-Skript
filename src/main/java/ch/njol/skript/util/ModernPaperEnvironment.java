package ch.njol.skript.util;

import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.Environment;
import io.papermc.lib.environments.PaperEnvironment;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link PaperEnvironment} for servers whose version string PaperLib cannot read.
 * <p>
 * PaperLib 1.0.8 parses the server version with a regex that only accepts a single digit major
 * version: {@code \(MC: (\d)\.(\d+)\.?(\d+?)?...\)}. A version such as {@code (MC: 26.1.2)} never
 * matches it, so PaperLib leaves its version at 0 and every {@code isVersion} call returns false.
 * {@link Environment} installs blocking fallbacks before those checks run and only replaces them
 * once a check passes, so on such a server every PaperLib feature silently stays on its fallback -
 * including {@code AsyncChunksSync}, which loads the chunk on the calling thread.
 * <p>
 * Skript asks PaperLib for a chunk in {@link ch.njol.skript.effects.EffTeleport}. On server forks
 * that tick worlds off the main thread, an event handler - and therefore that request - can run on
 * a world ticker thread. A blocking chunk load there deadlocks the server: finishing the load needs
 * the main thread, and the main thread is waiting for the very world tick that is blocked.
 * <p>
 * {@link PaperEnvironment} chooses its handlers by calling {@code isVersion} virtually while it is
 * constructed, so answering those calls correctly is enough to get the asynchronous handlers back.
 * Every branch it takes is guarded by a reflective method lookup, so reporting a high version can
 * only select handlers the server actually supports.
 * <p>
 * The version getters are left as PaperLib parsed them, which is 0. The numbers they report belong
 * to the 1.x scheme, and a server this class is installed on does not use it, so there is no honest
 * value to give them. Ask {@link #isVersion(int, int)} instead.
 */
public class ModernPaperEnvironment extends PaperEnvironment {

	private static final Pattern MINECRAFT_VERSION = Pattern.compile("\\(MC: (\\d+)\\.(\\d+)(?:\\.(\\d+))?\\)");

	private static final int MAJOR;
	private static final int MINOR;
	private static final int PATCH;

	static {
		int major = 1, minor = 0, patch = 0;
		Matcher matcher = MINECRAFT_VERSION.matcher(Bukkit.getVersion());
		if (matcher.find()) {
			major = Integer.parseInt(matcher.group(1));
			minor = Integer.parseInt(matcher.group(2));
			String patchGroup = matcher.group(3);
			if (patchGroup != null)
				patch = Integer.parseInt(patchGroup);
		}
		MAJOR = major;
		MINOR = minor;
		PATCH = patch;
	}

	/**
	 * Replaces PaperLib's environment when it is running on Paper but could not read the version.
	 * Servers PaperLib already understands are left alone, as are servers that are not Paper.
	 * <p>
	 * Must run before anything touches PaperLib for real work; {@link ch.njol.skript.effects.EffTeleport}
	 * reads the environment once, when its class is initialised.
	 */
	public static void install() {
		Environment current = PaperLib.getEnvironment();
		if (current instanceof ModernPaperEnvironment)
			return;
		// A version of 0 means the regex did not match, which is the only case we want to correct.
		if (!current.isPaper() || current.getMinecraftVersion() != 0)
			return;
		PaperLib.setCustomEnvironment(new ModernPaperEnvironment());
	}

	private ModernPaperEnvironment() { }

	@Override
	public boolean isVersion(int minor) {
		return isVersion(minor, 0);
	}

	@Override
	public boolean isVersion(int minor, int patch) {
		// PaperLib only ever asks about 1.x releases, so any later major version is past all of them.
		if (MAJOR != 1)
			return MAJOR > 1;
		return MINOR > minor || (MINOR == minor && PATCH >= patch);
	}

	@Override
	public String getName() {
		return "Paper (" + MAJOR + "." + MINOR + "." + PATCH + ")";
	}

}
