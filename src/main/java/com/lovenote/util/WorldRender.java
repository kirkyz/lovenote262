package com.lovenote.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class WorldRender {
	public static final List<Consumer<WorldRenderContext>> START = new ArrayList<>();
	public static final List<Consumer<WorldRenderContext>> LAST = new ArrayList<>();
	public static final List<Consumer<WorldRenderContext>> END = new ArrayList<>();

	private WorldRender() {}
}
