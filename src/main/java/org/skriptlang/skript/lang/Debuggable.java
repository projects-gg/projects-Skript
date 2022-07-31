package org.skriptlang.skript.lang;

import org.skriptlang.skript.lang.context.TriggerContext;

public interface Debuggable {

	String toString(TriggerContext context, boolean debug);

	@Override
	String toString();

}
