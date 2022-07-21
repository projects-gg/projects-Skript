package ch.njol.skript.variables2.storages;

import java.util.concurrent.ForkJoinPool;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import ch.njol.skript.variables2.StorageConfiguration;
import ch.njol.skript.variables2.VariableStorage;

/**
 * Variable storage implementation based on Caffeine library for ram caching.
 */
public class CaffeineVariableStorage extends VariableStorage {

	private final LoadingCache<String, Object> cache;

	public CaffeineVariableStorage(StorageConfiguration configuration) {
		super(configuration);
		cache = Caffeine.newBuilder()
				.executor(new ForkJoinPool()) // Use separate pool to not starve common one
				.build(new CacheLoader<>() {
					@Override
					@Nullable
					public Object load(String key) throws Exception {
						// TODO Auto-generated method stub
						return null;
					}
				});
	}

	@Override
	boolean initialize() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean requiresFile() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	@Nullable
	public Object getVariable(String name, @Nullable Event e, boolean local) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVariable(String name, @Nullable Event e, boolean local, @Nullable Object value) {
		// TODO Auto-generated method stub
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
	}

	@Override
	void onReload(StorageConfiguration configuration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
