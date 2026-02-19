# KMP circular task dependency (compileDesktopMainJava ↔ desktopJar)

**Last updated**: 2026-02-19

## What happens

In some Kotlin Multiplatform modules that use **`com.android.kotlin.multiplatform.library`** and a **JVM desktop** target (`jvm("desktop")`), Gradle reports a circular dependency between tasks:

```
:data:auth:compileDesktopMainJava
+--- :data:auth:desktopJar
|    +--- :data:auth:compileDesktopMainJava  ← cycle
|    +--- :data:auth:compileKotlinDesktop
|    \--- :data:auth:desktopMainClasses
\--- :data:auth:desktopJar (*)
```

- **compileDesktopMainJava**: compiles Java sources for the desktop (JVM) target. In our modules we only have Kotlin, so this task is effectively NO-SOURCE.
- **desktopJar**: builds the JAR for the desktop variant and depends on both Kotlin and Java compilation.
- The Android KMP library plugin wires `desktopJar` to depend on `compileDesktopMainJava`, and somewhere in the chain `compileDesktopMainJava` ends up depending on `desktopJar`, creating the cycle.

## Why it’s desktop-specific

The cycle appears only for the **desktop** (JVM) target because:

- The plugin adds a **Java** compilation task for the JVM target (`compileDesktopMainJava`).
- Other targets (Android, wasmJs, iOS) don’t create this same task/dependency chain.

So the issue is specific to how the JVM/desktop target is configured by the plugin, not to “desktop” as a product.

## What’s recommended on the internet

1. **Don’t remove `jvm("desktop")`**  
   If you need to run or assemble the app for desktop, other modules (e.g. `composeApp`) need the desktop variant of this library. Removing the desktop target would break desktop builds.

2. **Prefer fixing the structure**  
   General Gradle/KMP advice is to avoid circular *project* dependencies (e.g. A → B → A) by restructuring modules. Here the cycle is between **tasks** inside the same project, not between projects, so restructuring modules doesn’t apply.

3. **Workaround: break the task dependency**  
   The usual approach is to **remove the problematic task dependency** before the task graph is built:
   - Use **`afterEvaluate`** (not `gradle.taskGraph.whenReady`).
   - On the task that should not depend on the other (e.g. `desktopJar`), **filter out** the dependency on `compileDesktopMainJava` and set the new dependency set with **`setDependsOn(...)`** (see e.g. [Stack Overflow: how to remove task dependency](https://stackoverflow.com/questions/68800375/gradle-how-to-remove-task-dependency)).

4. **Android KMP plugin and Java**  
   For the **Android** target, the same plugin uses **opt-in Java** via `withJava()` in the `androidLibrary` block; by default it doesn’t enable Java. The cycle we see is for the **JVM/desktop** target, where the Kotlin/Android plugins still add a Java compile task and wire it into `desktopJar`.

## What we tried (and what does not work)

1. **Remove the dependency in `afterEvaluate`**  
   Get `desktopJar`, filter `dependsOn` to exclude `compileDesktopMainJava`, call `setDependsOn(filtered)`.  
   Result: the cycle still appears when building the task graph. So either the dependency is not only in `dependsOn`, or the graph is fixed before our callback runs.

2. **Remove the dependency from `compileDesktopMainJava`**  
   Same idea on the other task. Same result: cycle unchanged.

3. **Mutate `jarTask.dependsOn` (e.g. `remove(compileDesktopMainJava)`)**  
   Gradle fails with: **"Removing a task dependency from a task instance is not supported."** So we cannot remove a single dependency by mutating the set.

4. **Align with `:data:network`**  
   `:data:network` uses the same plugin and `jvm("desktop")` but does **not** show the cycle. We added the same source set layout and a `desktopMain` dependency in `:data:auth`; the cycle in `:data:auth` (and `:data:storage`) remained. So the cause is still unclear (e.g. number of dependencies, evaluation order).

## Related: domain:auth not resolving in data:auth

When `:data:auth` depends on `:domain:auth`, Gradle’s dependency resolution was **substituting** `project :domain:auth` with `project :data:auth` (so `:data:auth` ended up depending on itself for “domain” types). That happened because both projects default to the same **project name** (`"auth"`), and resolution was deduplicating by (group, name).

**Fix applied:**

1. **Distinct `group`** so resolution keeps both projects:
   - In **`domain/auth/build.gradle.kts`**: `afterEvaluate { group = "com.vaultstadio.domain" }`
   - In **`data/auth/build.gradle.kts`**: `afterEvaluate { group = "com.vaultstadio.data" }`
   - Do **not** change `project.name` in `settings.gradle.kts` (e.g. to `"domain-auth"`); other projects reference `project(":domain:auth")` by path, and renaming can break those references.

2. **`:data:auth`** needs **`kotlinx-datetime`** for DTOs/mappers: add `implementation(libs.kotlinx.datetime)` in `commonMain` in `data/auth/build.gradle.kts`.

3. **composeApp** must import the auth Koin module: in `composeApp/.../di/AppModule.kt` add  
   `import com.vaultstadio.app.data.auth.di.authModule` so `authModule()` resolves.

After the group fix, `:data:auth:compileKotlinDesktop` and `:composeApp:compileKotlinDesktop` succeed. The task cycle described above may have been aggravated by the wrong resolution; if it reappears on other targets, the recommendations in “What we do” still apply.

## What we do in this project

- **`:data:network`**: builds for desktop without changes.
- **`:data:auth`**: after applying the **group** fix (and kotlinx-datetime + authModule import), desktop build succeeds. If the task cycle reappears, see “Recommended next steps” below.
- **`:data:storage`**: if it shows the same cycle, apply the same group/disambiguation pattern if it has a sibling `:domain:storage`.
- **Recommended next steps** (if the cycle reappears):  
  - Report the cycle to the [Android Kotlin Multiplatform Library](https://developer.android.com/kotlin/multiplatform/plugin) or Kotlin plugin issue tracker, with a minimal sample.  
  - Try a different version of the plugin or Gradle.  
  - As a last resort, avoid applying the Android KMP library plugin to modules that only need `commonMain` + desktop (if that setup is supported and doesn’t break other targets).

## References

- [Gradle: Resolve circular dependency](https://stackoverflow.com/questions/58178381/resolve-circular-dependency-in-gradle/58186686)
- [Gradle: how to remove task dependency](https://stackoverflow.com/questions/68800375/gradle-how-to-remove-task-dependency)
- Kotlin Multiplatform: [Configure compilations](https://kotlinlang.org/docs/multiplatform-configure-compilations.html)
- Android: [Set up the Android Gradle Library Plugin for KMP](https://developer.android.com/kotlin/multiplatform/plugin) (opt-in Java for Android target)
