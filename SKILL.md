---
name: compose-ui-control
description: Control a running Compose Desktop application via HTTP. Use when you need to interact with UI elements, click buttons, enter text, wait for elements to appear, or capture screenshots in a Compose Desktop app that has compose-ui-test-server enabled.
argument-hint: "[action] [target]"
---

# Compose UI Test Server - Agent Control

This skill enables you to control Compose Desktop applications at runtime via HTTP when `compose-ui-test-server` is integrated.

## Setting Up a New Project

To add agent control capabilities to a Compose Desktop project:

### 1. Add the dependency

In the app's `build.gradle.kts`, add to the desktop source set:

```kotlin
kotlin {
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation("io.github.forketyfork:compose-ui-test-server:0.1.0")
                // Also need compose.uiTest for the test framework
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}
```

For projects using version catalogs, add to `libs.versions.toml`:

```toml
[libraries]
compose-ui-test-server = { module = "io.github.forketyfork:compose-ui-test-server", version = "0.1.0" }
```

### 2. Update the main function

Replace the standard Compose Desktop `main()` with `runApplication`:

```kotlin
import io.github.forketyfork.composeuittest.WindowConfig
import io.github.forketyfork.composeuittest.runApplication

fun main() =
    runApplication(
        windowConfig = WindowConfig(
            title = "My App",
            minimumWidth = 1024,
            minimumHeight = 768,
        ),
    ) {
        App()
    }
```

### 3. Add test tags to UI elements

For agents to interact with UI elements, add test tags:

```kotlin
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

Button(
    onClick = { /* ... */ },
    modifier = Modifier.testTag("login_button")
) {
    Text("Login")
}

TextField(
    value = username,
    onValueChange = { username = it },
    modifier = Modifier.testTag("username_field")
)
```

## Discovering Server Support

Check if the project uses this library:

```bash
grep -r "compose-ui-test-server\|composeuittest\|runApplication\|startTestServer" --include="*.gradle*" --include="*.kt" .
```

Look for:
- Dependency on `compose-ui-test-server` module
- Imports from `io.github.forketyfork.composeuittest`
- Calls to `runApplication`, `startTestServer`, or `startTestServerIfEnabled`

## Starting the Application

### Standard launcher (recommended):

```bash
# Normal mode
./gradlew run

# Agent-controlled mode
./gradlew run -Dcompose.ui.test.server.enabled=true

# With custom port
./gradlew run -Dcompose.ui.test.server.enabled=true -Dcompose.ui.test.server.port=8080
```

### If a dedicated Gradle task exists:

```bash
./gradlew runInteractiveTest
```

Default port is **54345**.

## Verifying the Server

Always check health first:

```bash
curl http://localhost:54345/health
# Expected: "OK"
```

## Available Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Health check |
| `GET /onNodeWithTag/{tag}/performClick` | Click element by test tag |
| `GET /onNodeWithTag/{tag}/performTextInput?text=...` | Enter text (URL-encode the text!) |
| `GET /onNodeWithText/{text}/performClick` | Click element by display text |
| `GET /waitUntilExactlyOneExists/tag/{tag}?timeout=5000` | Wait for element by tag |
| `GET /waitUntilExactlyOneExists/text/{text}?exact=true&timeout=5000` | Wait for element by text |
| `GET /waitForIdle` | Wait for UI to stabilize |
| `GET /captureScreenshot?path=/tmp/screenshot.png` | Capture screenshot |

## Workflow Pattern

Follow this sequence for reliable interactions:

```bash
# 1. Verify server is running
curl http://localhost:54345/health

# 2. Wait for UI to be ready
curl http://localhost:54345/waitForIdle

# 3. Perform action
curl "http://localhost:54345/onNodeWithTag/username/performTextInput?text=myuser"

# 4. Wait for UI to settle
curl http://localhost:54345/waitForIdle

# 5. Perform next action
curl http://localhost:54345/onNodeWithTag/login_button/performClick

# 6. Wait for result
curl "http://localhost:54345/waitUntilExactlyOneExists/tag/dashboard?timeout=10000"

# 7. Capture screenshot to verify
curl "http://localhost:54345/captureScreenshot?path=/tmp/result.png"
```

## Finding Test Tags

Search the codebase for test tags:

```bash
grep -r "testTag\|Modifier.testTag" --include="*.kt" .
```

Also check project documentation:
- `CLAUDE.md`
- Test files in `src/*Test/` directories

## Important Notes

- **Always URL-encode** special characters in text: space→`%20`, `@`→`%40`, `&`→`%26`
- **Use `waitForIdle`** between operations for stability
- **Check HTTP status codes**: 200=success, 400=bad request, 500=error
- **Use appropriate timeouts** for waits (default 5000ms may be too short)
- Screenshots use absolute paths

## Error Handling

If an endpoint returns an error:
1. Check the element exists (search for its test tag in code)
2. Ensure UI has finished loading (`waitForIdle`)
3. Verify the server is still running (`/health`)
4. Try with a longer timeout for wait operations
