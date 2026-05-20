# AskChat

AskChat is a Minecraft plugin that allows players to interact with Google's Gemini Pro AI directly in the game chat. Simply type `!ask` followed by your question, and the AI will provide a helpful response.

## Examples

![Screenshot 2024-08-08 014911](https://github.com/user-attachments/assets/d4030652-42b9-4b1a-9b22-ccbef5f28932)

![Screenshot 2024-08-08 015430](https://github.com/user-attachments/assets/94ad5277-029a-4a00-93e3-c13411f22cc9)

## Features

* **Chat-Based Interface:** Ask questions and receive answers without leaving your Minecraft world.
* **Configurable:**
    * Customize the AI's prompt to guide its responses.
    * Change the bot's name in chat.
    * Optionally broadcast both the question and the answer to the server.
* **Powered by Google Gemini Pro:** Tap into the advanced capabilities of Gemini Pro for intelligent and relevant answers.
* **Lightweight:** Designed to minimize impact on server performance.

## Installation

1. **Obtain a Google Gemini API Key:** Follow the instructions at [https://ai.google.dev/gemini-api/docs/api-key](https://ai.google.dev/gemini-api/docs/api-key) to obtain your API key. 
2. **Download:** Get the latest `AiChatBuddy.jar` file from the [Releases](link-to-your-releases-page) section.
3. **Install:** Place the JAR file in your Minecraft server's `plugins` folder.
4. **Configure:** Edit the `config.yml` file in the plugin's folder:
    * **`api-key`:**  Enter your Google Gemini Pro API key.
    * **`prompt-template`:** (Optional) Customize the AI's prompt.
    * **`api-url`:** (Optional) Modify the API endpoint if needed.
    * **`bot-name`:**  Choose the name for the AI in chat (default: "Notch").
    * **`private-questions`:** (Optional) Set to `true` for private responses (default: `false`). 
5. **Restart:** Restart your server to enable the plugin.

## Usage

Type `!ask` followed by your question in the chat, e.g., `!ask How do I make a crafting table?`. The AI will respond with an answer.

## Support & Feedback

If you have any questions, issues, or feature requests, please open an issue on [GitHub](link-to-your-github-repo).

## Contributing

Contributions are welcome! Please feel free to fork this repository and submit pull requests.

## License

This project is licensed under the [MIT License](LICENSE).
# AiChatBuddy

An AI chat assistant for PaperMC servers. Players ask questions in chat with `!ask <question>` and a configurable AI persona ("Notch" by default) answers, optionally calling tools to fetch live information.

Supports three providers — **Gemini** (default), **Ollama** (local or cloud), and **NVIDIA NIM** — with agentic tool use for **web search** (Brave) and the **Minecraft Wiki**.

---

## Requirements

- Paper **26.1.2** or newer (Minecraft 1.22 line).
- Java **25**.

---

## Installation

1. Drop the matching jar into your server's `plugins/` directory.
2. Start the server once to generate `plugins/AiChatBuddy/config.yml`.
3. Stop the server, edit `config.yml` (see below), and start again — or edit live and run `/aichat reload`.

---

## Configuration

Default `config.yml`:

```yaml
provider: gemini          # gemini | ollama | nvidia

bot-name: "Notch"
private-questions: false  # true = whisper replies, false = broadcast
max-history: 8            # rolling per-player message history; 0 = stateless
agent-max-steps: 8        # max tool-calling iterations before forcing a best-effort answer

prompt-template: |
  You are Notch — the ancient architect and silent overseer ...
  (full Notch persona, see config.yml)

providers:
  gemini:
    api-key: ""
    api-url: "https://generativelanguage.googleapis.com/v1beta/models/gemma-4-31b-it:generateContent"
    temperature: 0.5
    top-p: 0.99
  ollama:
    base-url: "http://localhost:11434"   # or https://ollama.com for cloud
    api-key: ""                          # cloud only
    model: "gpt-oss:20b"
    temperature: 0.7
  nvidia:
    base-url: "https://integrate.api.nvidia.com"
    api-key: ""                          # must start with nvapi-
    model: "meta/llama-3.3-70b-instruct"
    temperature: 0.5

tools:
  web-search:
    enabled: true
    api-key: ""           # Brave Search API key; tool is hidden from the LLM if blank
    max-results: 3
  minecraft-wiki:
    enabled: true
    max-results: 3
```

You only need to fill in the api-keys for the provider(s) and tool(s) you want to use. Unconfigured providers/tools are simply skipped.

---

## Getting API keys

### Gemini (default, free tier available)

1. Go to <https://aistudio.google.com/apikey>.
2. Sign in with a Google account.
3. Click **Create API key**.
4. Copy the key (starts with `AIza…`) into `providers.gemini.api-key`.

Switch models by editing `providers.gemini.api-url` — the URL contains the model name. Defaults to `gemma-4-31b-it` (function-calling capable, fast, free). Other good choices:

- `gemini-2.5-flash` — fast, low-cost, strong tool use.
- `gemini-2.5-pro` — better quality, slower.
- `gemma-4-26b-a4b-it` — bigger context window, only ~4B active params per token.

### Brave Search (for the `web_search` tool)

1. Go to <https://brave.com/search/api/>.
2. Click **Get Started** and sign up.
3. Subscribe to the **Free** plan (1 query/sec, 2k queries/month).
4. In the dashboard, create an API key under **API Keys**.
5. Paste it into `tools.web-search.api-key`. Leave blank to disable web search.

### NVIDIA NIM (free tier)

1. Go to <https://build.nvidia.com/>.
2. Sign in with NVIDIA Developer credentials.
3. Open any model (e.g. <https://build.nvidia.com/meta/llama-3_3-70b-instruct>).
4. Click **Get API Key** in the top right.
5. Copy the key (starts with `nvapi-`) into `providers.nvidia.api-key`.

Recommended models for `providers.nvidia.model`:
- `meta/llama-3.3-70b-instruct` (default)
- `nvidia/llama-3.1-nemotron-70b-instruct`
- `mistralai/mixtral-8x22b-instruct-v0.1`

### Ollama

You can run Ollama **locally** (free, private) or use Ollama cloud.

**Local:**
1. Install Ollama from <https://ollama.com/download>.
2. Pull a model that supports tool calls, e.g. `ollama pull llama3.1` or `ollama pull qwen2.5`.
3. In `config.yml`:
   - `providers.ollama.base-url: "http://localhost:11434"`
   - `providers.ollama.api-key: ""` (not needed locally)
   - `providers.ollama.model: "llama3.1"` (or whichever you pulled)

**Cloud :**
1. Sign up at <https://ollama.com/>.
2. Generate an API key under your account settings.
3. In `config.yml`:
   - `providers.ollama.base-url: "https://ollama.com"`
   - `providers.ollama.api-key: "<your-key-key>"`
   - `providers.ollama.model: "gpt-oss:20b"` (or `gpt-oss:120b`, `qwen3-coder:480b`, `deepseek-v3.1:671b`, `glm-4.7`, `kimi-k2.6`, etc.)

Switch providers at any time by editing `provider:` and running `/aichat reload`.

---

## Commands

| Command | Permission | Description |
| ------- | ---------- | ----------- |
| `!ask <question>` | `aichatbuddy.ask` (default: everyone) | Ask the AI in chat. |
| `/aichat reload` | `aichatbuddy.reload` (default: op) | Reload `config.yml` and clear in-memory history. |
| `/aichat forget` | `aichatbuddy.forget.self` (default: everyone) | Clear your own conversation history. |
| `/aichat forget <player>` | `aichatbuddy.forget.others` (default: op) | Clear another player's history. |

---

## Building from source

Requires JDK 25. The Foojay toolchain plugin auto-downloads it if missing.

```powershell
./gradlew clean build
```

Output: `build/libs/AiChatBuddy-<version>.jar`.

---

## How tool calling works

When the bot decides a tool is needed it emits a function call; the plugin runs the tool, feeds the result back, and lets the model loop until it either (a) emits a text reply or (b) hits `agent-max-steps`. If the limit is hit, the plugin asks the model to synthesise a best-effort answer with what it already has — players never see "I ran out of tool calls" errors.

Tool results are intentionally **not** persisted in per-player history. Only the player's question and the final assistant reply are stored, which prevents stale tool transcripts from polluting future turns and avoids the Gemini `function_response must immediately follow function_call` error class entirely.

---

## License

See repository for license details.
