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

## What we do in this project

- **`:data:network`**: builds for desktop without changes.
- **`:data:auth`** and **`:data:storage`**: currently hit the cycle when running `:data:auth:compileKotlinDesktop` or `:data:storage:compileKotlinDesktop`.  
- **Recommended next steps**:  
  - Report the cycle to the [Android Kotlin Multiplatform Library](https://developer.android.com/kotlin/multiplatform/plugin) or Kotlin plugin issue tracker, with a minimal sample.  
  - Try a different version of the plugin or Gradle.  
  - As a last resort, avoid applying the Android KMP library plugin to modules that only need `commonMain` + desktop (if that setup is supported and doesn’t break other targets).

## References

- [Gradle: Resolve circular dependency](https://stackoverflow.com/questions/58178381/resolve-circular-dependency-in-gradle/58186686)
- [Gradle: how to remove task dependency](https://stackoverflow.com/questions/68800375/gradle-how-to-remove-task-dependency)
- Kotlin Multiplatform: [Configure compilations](https://kotlinlang.org/docs/multiplatform-configure-compilations.html)
- Android: [Set up the Android Gradle Library Plugin for KMP](https://developer.android.com/kotlin/multiplatform/plugin) (opt-in Java for Android target)
