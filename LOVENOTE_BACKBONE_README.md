# Lovenote 26.2 legacy-module backbone

Compared old ModuleManager registrations against the existing `com.lovenote.module.impl` files.

- Old module registrations: **162**
- Names already present in new custom code: **10**
- New registry-only placeholders: **152**
- Total new custom module Java files: **179**

**Critical:** The 152 new classes are **not feature implementations**. They extend `UnportedModule`, cannot be enabled, and deliberately do nothing. This avoids claiming that legacy behavior was successfully ported when the APIs and hooks have not been adapted. Existing modules were left unchanged.

The original old client source is preserved in `legacy-1.21-reference/` (outside the Java source set) for implementation work. `LOVENOTE_BACKBONE_MANIFEST.json` lists every placeholder, category, and alias.

Hypixel and Donut names are included with their original category/alias associations where present in the old ModuleManager. Their functionality is **not yet ported**.

## Validate on Java 25

```bash
java -version
./gradlew compileJava --stacktrace --console=plain
./gradlew build --console=plain
```

A successful build would only establish that the scaffolding compiles; each actual port still requires behavior and runtime tests.
