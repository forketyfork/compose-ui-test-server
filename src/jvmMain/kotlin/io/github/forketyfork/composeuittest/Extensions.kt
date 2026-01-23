package io.github.forketyfork.composeuittest

import androidx.compose.ui.test.ComposeUiTest

/**
 * System property to enable the UI test server.
 * Set to "true" to enable: `-Dcompose.ui.test.server.enabled=true`
 */
const val PROPERTY_SERVER_ENABLED = "compose.ui.test.server.enabled"

/**
 * System property to configure the server port.
 * Example: `-Dcompose.ui.test.server.port=8080`
 */
const val PROPERTY_SERVER_PORT = "compose.ui.test.server.port"

/**
 * Check if the UI test server is enabled via system property.
 *
 * @return true if [PROPERTY_SERVER_ENABLED] is set to "true"
 */
fun isTestServerEnabled(): Boolean = System.getProperty(PROPERTY_SERVER_ENABLED, "false").toBoolean()

/**
 * Get the configured server port from system property.
 *
 * @param default The default port if not configured
 * @return The port from [PROPERTY_SERVER_PORT] or the default
 */
fun getTestServerPort(default: Int = 54345): Int = System.getProperty(PROPERTY_SERVER_PORT)?.toIntOrNull() ?: default

/**
 * Start a UI test server for this Compose UI test.
 *
 * This creates and starts a [ComposeUiTestServer] that enables coding agents
 * and automation tools to control the running application via HTTP.
 *
 * Example usage:
 * ```kotlin
 * runComposeUiTest {
 *     setContent { MyApp() }
 *
 *     val server = startTestServer(
 *         config = ComposeUiTestServerConfig(port = 8080)
 *     )
 *
 *     server.awaitTermination()
 * }
 * ```
 *
 * @param config Server configuration (uses defaults if not specified)
 * @return The started [ComposeUiTestServer] instance
 */
fun ComposeUiTest.startTestServer(
    config: ComposeUiTestServerConfig = ComposeUiTestServerConfig(),
): ComposeUiTestServer = ComposeUiTestServer(this, config).start()

/**
 * Conditionally start a UI test server based on system property.
 *
 * The server is only started if the system property [PROPERTY_SERVER_ENABLED]
 * is set to "true". This allows the same application binary to run normally
 * or in agent-controlled mode based on JVM arguments.
 *
 * The port can be configured via [PROPERTY_SERVER_PORT] system property,
 * or falls back to the config value.
 *
 * Example usage in your app:
 * ```kotlin
 * runComposeUiTest {
 *     setContent { MyApp() }
 *
 *     // Server only starts if -Dcompose.ui.test.server.enabled=true
 *     val server = startTestServerIfEnabled()
 *
 *     // Continue with normal app flow...
 *     // If server was started, agents can control the app via HTTP
 * }
 * ```
 *
 * Run with agent control enabled:
 * ```bash
 * java -Dcompose.ui.test.server.enabled=true -Dcompose.ui.test.server.port=8080 -jar myapp.jar
 * ```
 *
 * @param config Base server configuration (port may be overridden by system property)
 * @return The started [ComposeUiTestServer] if enabled, null otherwise
 */
fun ComposeUiTest.startTestServerIfEnabled(
    config: ComposeUiTestServerConfig = ComposeUiTestServerConfig(),
): ComposeUiTestServer? {
    if (!isTestServerEnabled()) {
        return null
    }
    val port = getTestServerPort(config.port)
    val effectiveConfig = if (port != config.port) config.copy(port = port) else config
    return ComposeUiTestServer(this, effectiveConfig).start()
}
