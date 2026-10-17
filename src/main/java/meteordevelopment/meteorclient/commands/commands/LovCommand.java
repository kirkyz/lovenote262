/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/lovenote262).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.lovenote.Lovenote;
import com.lovenote.config.LovenoteConfig;
import com.lovenote.module.Module;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.Locale;

public class LovCommand extends Command {
    public LovCommand() {
        super("lov", "Lovenote module control", "lovenote");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> { list(); return SINGLE_SUCCESS; });

        builder.then(literal("list").executes(_ -> { list(); return SINGLE_SUCCESS; }));

        builder.then(literal("save").executes(_ -> {
            LovenoteConfig.save(true);
            info("Saved.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("toggle")
            .then(argument("module", StringArgumentType.word()).executes(ctx -> {
                Module m = find(StringArgumentType.getString(ctx, "module"));
                if (m == null) { info("No module matching that."); return SINGLE_SUCCESS; }
                m.toggle();
                info("%s is now %s", m.name, m.enabled ? "on" : "off");
                LovenoteConfig.save(true);
                return SINGLE_SUCCESS;
            })));

        builder.then(literal("bind")
            .then(argument("module", StringArgumentType.word())
                .then(argument("key", StringArgumentType.word()).executes(ctx -> {
                    Module m = find(StringArgumentType.getString(ctx, "module"));
                    if (m == null) { info("No module matching that."); return SINGLE_SUCCESS; }
                    String raw = StringArgumentType.getString(ctx, "key");
                    int k = parseKey(raw);
                    m.key = k;
                    info("%s bound to %s", m.name, k < 0 ? "none" : raw);
                    LovenoteConfig.save(true);
                    return SINGLE_SUCCESS;
                }))));
    }

    private void list() {
        if (Lovenote.modules == null) { info("Not initialised yet."); return; }
        StringBuilder sb = new StringBuilder();
        for (Module m : Lovenote.modules.all()) {
            sb.append('\n').append(m.enabled ? "+ " : "- ").append(m.name);
            if (m.key >= 0) sb.append("  [key ").append(m.key).append(']');
        }
        info("Modules:%s", sb);
    }

    private static Module find(String q) {
        if (Lovenote.modules == null) return null;
        String s = q.toLowerCase(Locale.ROOT);
        for (Module m : Lovenote.modules.all()) if (m.name.toLowerCase(Locale.ROOT).equals(s)) return m;
        for (Module m : Lovenote.modules.all()) if (m.name.toLowerCase(Locale.ROOT).contains(s)) return m;
        return null;
    }

    private static int parseKey(String s) {
        s = s.trim().toLowerCase(Locale.ROOT);
        if (s.equals("none") || s.equals("-") || s.equals("off")) return -1;

        for (String cand : new String[] { "key.keyboard." + s, s }) {
            try {
                InputConstants.Key k = InputConstants.getKey(cand);
                if (k != null && k != InputConstants.UNKNOWN && k.getValue() > 0) return k.getValue();
            } catch (Exception ignored) { }
        }
        return -1;
    }
}
