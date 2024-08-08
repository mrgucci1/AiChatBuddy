# AiChatBuddy

AiChatBuddy is a Minecraft plugin that allows players to interact with Google's Gemini Pro AI directly in the game chat. Simply type `!ask` followed by your question, and the AI will provide a helpful response.

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

1. **Download:** Get the latest `AiChatBuddy.jar` file from the [Releases](link-to-your-releases-page) section.
2. **Install:** Place the JAR file in your Minecraft server's `plugins` folder.
3. **Configure:** Edit the `config.yml` file in the plugin's folder:
    * **`api-key`:**  Enter your Google Gemini Pro API key.
    * **`prompt-template`:** (Optional) Customize the AI's prompt.
    * **`api-url`:** (Optional) Modify the API endpoint if needed.
    * **`bot-name`:**  Choose the name for the AI in chat (default: "Notch").
    * **`private-questions`:** (Optional) Set to `true` for private responses (default: `false`). 
4. **Restart:** Restart your server to enable the plugin.

## Usage

Type `!ask` followed by your question in the chat, e.g., `!ask How do I make a crafting table?`. The AI will respond with an answer.

## Support & Feedback

If you have any questions, issues, or feature requests, please open an issue on [GitHub](link-to-your-github-repo).

## Contributing

Contributions are welcome! Please feel free to fork this repository and submit pull requests.

## License

This project is licensed under the [MIT License](LICENSE).
