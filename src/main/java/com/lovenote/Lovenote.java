package com.lovenote;

import com.lovenote.module.ModuleManager;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.api.ClientModInitializer;

import java.lang.invoke.MethodHandles;

public class Lovenote implements ClientModInitializer {
	public static ModuleManager modules;

	@Override
	public void onInitializeClient() {
		MeteorClient.EVENT_BUS.registerLambdaFactory("com.lovenote",
				(lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

		modules = new ModuleManager();
		MeteorClient.EVENT_BUS.subscribe(LovenoteEvents.class);
	}
}
