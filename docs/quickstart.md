# Your first dui plugin

This example embeds dui in an ordinary Paper plugin. `/exampleui` opens a template with a counter; clicking its button updates the same session. It requires Java 25, Paper/Minecraft 26.2 and the matching dui resource pack. Clients need no mod.

## Prepare the library and pack

From the dui checkout:

```sh
./gradlew build publishToMavenLocal
./gradlew :dui-pack:generatePack -PminecraftJar=/path/to/minecraft-26.2-client.jar
```

The generator accepts the verified official 26.2 client JAR pinned in `dui-pack/src/main/resources/dui/minecraft.json`. It writes `dui-pack/build/pack/dui.zip` and `dui.json`. Serve the ZIP from your HTTP(S) endpoint. Keep these two outputs together: the metadata contains the pack hash, model registry, version and font metrics.

There is currently no public Maven release. `publishToMavenLocal` supplies the dependency for local builds. Copy the Gradle wrapper files from dui into your new plugin project, then add these files.

## Project files

`settings.gradle`:

```groovy
rootProject.name = 'example-ui'
```

`build.gradle`:

```groovy
plugins { id 'java' }
group = 'example.dui'
version = '0.1.0'
repositories {
    mavenLocal()
    mavenCentral()
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
}
dependencies {
    implementation 'gg.kembel.dui:dui-paper:0.1.0-SNAPSHOT'
    compileOnly 'io.papermc.paper:paper-api:26.2.build.129-stable'
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
tasks.withType(JavaCompile).configureEach { options.release = 25 }
jar {
    from { configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) } }
    exclude 'META-INF/*.SF', 'META-INF/*.RSA', 'META-INF/*.DSA', '**/module-info.class'
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

This small fat-JAR example embeds dui-core, dui-paper and Gson. Paper and Adventure stay provided by the server. For distribution, preserve dependency notices and include Gson's Apache 2.0 license, as demonstrated in dui-demo's `META-INF/licenses/` resources. A production shading setup can relocate Java packages; preserve `/ui` resources and the `dui:` pack namespace. `dui-pack` is build tooling and does not belong in the plugin's runtime JAR.

`src/main/resources/plugin.yml`:

```yaml
name: ExampleUi
version: 0.1.0
main: example.dui.ExampleUiPlugin
api-version: '26.2'
commands:
  exampleui:
    description: Open the example dui menu
```

`src/main/resources/config.yml`:

```yaml
pack-url: https://your-server.example/dui.zip
```

Replace the example URL with the endpoint serving your generated ZIP. dui requests the pack from the client; it does not host the ZIP itself. The local dui-demo plugin contains a loopback HTTP-server example.

`src/main/resources/ui/menu.html`:

```xml
<dui-menu width="300" height="126" padding="9" theme="studio">
  <dui-column gap="9">
    <dui-heading label="Hello {{name}}" height="18" />
    <dui-text label="Clicks: {{count}}" height="18" />
    <dui-text label="The server owns this counter." height="18" />
    <dui-button id="increment" action="increment" label="Add one" height="27" />
  </dui-column>
</dui-menu>
```

`src/main/java/example/dui/ExampleUiPlugin.java`:

```java
package example.dui;

import gg.kembel.dui.core.MenuTemplate;
import gg.kembel.dui.paper.ActionContext;
import gg.kembel.dui.paper.DialogOptions;
import gg.kembel.dui.paper.Dui;
import gg.kembel.dui.paper.PackDescriptor;
import gg.kembel.dui.paper.PackMetadata;
import gg.kembel.dui.paper.ViewModel;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExampleUiPlugin extends JavaPlugin {
    private final Map<UUID, Integer> counts = new HashMap<>();
    private final DialogOptions options = DialogOptions.notice("Counter", "Close", "close");
    private Dui ui;
    private MenuTemplate menu;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            var metadata = PackMetadata.read(getDataFolder().toPath().resolve("pack/dui.json"));
            var uri = URI.create(Objects.requireNonNull(getConfig().getString("pack-url")));
            ui = Dui.create(this, PackDescriptor.of(uri, metadata), metadata);
            try (var input = Objects.requireNonNull(getResource("ui/menu.html"))) {
                menu = ui.compile(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
            Objects.requireNonNull(getCommand("exampleui")).setExecutor((sender, command, label, args) -> {
                if (sender instanceof Player player) {
                    int count = counts.getOrDefault(player.getUniqueId(), 0);
                    ui.open(player, menu, view(player, count), options, this::handle);
                }
                return true;
            });
        } catch (Exception error) {
            getLogger().log(Level.SEVERE, "Example UI startup failed", error);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private ViewModel view(Player player, int count) {
        return ViewModel.data(Map.of("name", player.getName(), "count", count));
    }

    private void handle(ActionContext context) {
        if (!context.action().equals("increment")) return;
        Player player = context.player();
        int count = counts.merge(player.getUniqueId(), 1, Integer::sum);
        context.session().update(menu, view(player, count), options, this::handle);
    }

    @Override
    public void onDisable() {
        if (ui != null) ui.close();
        counts.clear();
    }
}
```

The counter is an in-memory example, reset when the plugin stops. Your own application supplies permissions, validation and persistence. The close action closes the session before calling `handle`; the handler returns immediately for it.

## Deploy and try it

```sh
./gradlew build
```

Copy `build/libs/example-ui-0.1.0.jar` into the server's `plugins/` directory. Copy the generated matching `dui.json` into `plugins/ExampleUi/pack/dui.json` before starting the server. On first startup, edit `plugins/ExampleUi/config.yml` with the reachable pack URL, or replace the example URL in the resource before building. Restart the server after deployment.

Join with an unmodified 26.2 client and run `/exampleui`. Accept the pack. The library waits until it loads before showing the menu. Click **Add one** repeatedly and then **Close**. Refusing or failing to load the pack prevents the UI from displaying.

Layouts use GUI pixels, not physical screen pixels. See [components](components.md) for layout and binding rules, [the LLM guide](llm-guide.md) for the API contract and known limits, and [dui-demo](https://github.com/Kembel-Entertainment/dui-demo) for native items, forms, images, animation and complete client tests.
