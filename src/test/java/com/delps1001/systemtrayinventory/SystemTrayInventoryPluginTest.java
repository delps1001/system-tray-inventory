package com.delps1001.systemtrayinventory;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

@SuppressWarnings("unchecked")
public class SystemTrayInventoryPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SystemTrayInventoryPlugin.class);
		RuneLite.main(args);
	}
}