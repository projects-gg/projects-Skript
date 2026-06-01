package org.skriptlang.skript.test.tests.syntaxes.expressions;

import ch.njol.skript.command.CommandEvent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.test.runner.SkriptJUnitTest;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ExprNameTest extends SkriptJUnitTest {

	@After
	public void cleanupParser() {
		ParserInstance.get().reset();
	}

	@Test
	public void commandSenderDisplayNamePrefersPlayer() {
		Component playerDisplayName = Component.text("Player Display Name");
		Component inventoryTitle = Component.text("Crafting");

		Player player = EasyMock.niceMock(Player.class);
		PlayerInventory inventory = EasyMock.niceMock(PlayerInventory.class);
		InventoryView inventoryView = EasyMock.niceMock(InventoryView.class);

		EasyMock.expect(player.displayName()).andReturn(playerDisplayName).anyTimes();
		EasyMock.expect(player.getInventory()).andReturn(inventory).anyTimes();
		EasyMock.expect(player.getOpenInventory()).andReturn(inventoryView).anyTimes();
		EasyMock.expect(inventory.getViewers()).andReturn(List.of(player)).anyTimes();
		EasyMock.expect(inventoryView.title()).andReturn(inventoryTitle).anyTimes();
		EasyMock.replay(player, inventory, inventoryView);

		ParserInstance.get().setCurrentEvent("command", CommandEvent.class);
		Expression<? extends Component> expression = new SkriptParser("sender's display name")
			.parseExpression(Component.class);

		Assert.assertNotNull(expression);
		Assert.assertEquals(
			playerDisplayName,
			expression.getSingle(new CommandEvent(player, "displaynametest", new String[0]))
		);
	}

}
