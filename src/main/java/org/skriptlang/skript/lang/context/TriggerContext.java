package org.skriptlang.skript.lang.context;

public interface TriggerContext {

	String getName();

	static DummyContext dummy() {
		return new DummyContext();
	}

	final class DummyContext implements TriggerContext {
		@Override
		public String getName() {
			return "dummy";
		}
	}

}
