package io.github.mrgucci1.aiChatBuddy.commands;

import io.github.mrgucci1.aiChatBuddy.AiChatBuddy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;


public class AiChatCommand implements CommandExecutor {

    private final AiChatBuddy plugin;

    public AiChatCommand(AiChatBuddy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /aichat <reload|forget [player]>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("aichatbuddy.reload")) {
                    sender.sendMessage(Component.text("You don't have permission to reload AiChatBuddy.", NamedTextColor.RED));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(Component.text("AiChatBuddy reloaded successfully.", NamedTextColor.GREEN));
                return true;

            case "forget":
                if (args.length >= 2) {
                    // forget <player>
                    if (!sender.hasPermission("aichatbuddy.forget.others")) {
                        sender.sendMessage(Component.text("You don't have permission to clear other players' history.", NamedTextColor.RED));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Component.text("Player '" + args[1] + "' not found.", NamedTextColor.RED));
                        return true;
                    }
                    plugin.getConversationManager().clear(target.getUniqueId());
                    sender.sendMessage(Component.text("Cleared chat history for " + target.getName() + ".", NamedTextColor.GREEN));
                } else {
                    // forget self
                    if (!sender.hasPermission("aichatbuddy.forget.self")) {
                        sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text("Only players can forget their own history.", NamedTextColor.RED));
                        return true;
                    }
                    UUID playerId = ((Player) sender).getUniqueId();
                    plugin.getConversationManager().clear(playerId);
                    sender.sendMessage(Component.text("Your chat history with the AI has been cleared.", NamedTextColor.GREEN));
                }
                return true;

            default:
                sender.sendMessage(Component.text("Unknown sub-command. Usage: /aichat <reload|forget [player]>", NamedTextColor.RED));
                return true;
        }
    }
}
