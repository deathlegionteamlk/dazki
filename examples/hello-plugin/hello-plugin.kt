// Example plugin: hello-plugin
//
// Demonstrates how to write a dazki plugin. The plugin contributes
// one RPC method, hello.greet, that takes a name and returns a
// greeting. The method requires the "packages.read" scope just to
// show how scopes work; in real life it would not need any scope.
//
// Build with:
//   kotlinc -d hello-plugin.jar hello-plugin.kt
//   d8 --output hello-plugin.dex hello-plugin.jar
//   echo "dev.deathlegion.dazki.examples.HelloPlugin" > hello-plugin.txt
//   adb push hello-plugin.dex hello-plugin.txt /data/local/tmp/dazki-plugins/
//   adb shell pkill -f DazkiServerMain  # restart the server

package dev.deathlegion.dazki.examples

import dev.deathlegion.dazki.plugin.DazkiPlugin
import dev.deathlegion.dazki.plugin.PluginContext
import dev.deathlegion.dazki.plugin.PluginMethod

class HelloPlugin : DazkiPlugin {
    override val id: String = "hello"
    override val displayName: String = "Hello plugin"

    private lateinit var ctx: PluginContext

    override fun onInit(ctx: PluginContext) {
        this.ctx = ctx
        ctx.log("INFO", "hello plugin initialized")
    }

    override fun onShutdown() {
        ctx.log("INFO", "hello plugin shutting down")
    }

    override fun methods(): List<PluginMethod> = listOf(
        PluginMethod(
            name = "hello.greet",
            scopes = setOf("packages.read"),
            handler = { args ->
                val name = args["name"] as? String ?: "world"
                mapOf("greeting" to "hello, $name")
            }
        ),
        PluginMethod(
            name = "hello.time",
            scopes = setOf("packages.read"),
            handler = { _ ->
                mapOf("now" to System.currentTimeMillis())
            }
        )
    )
}
