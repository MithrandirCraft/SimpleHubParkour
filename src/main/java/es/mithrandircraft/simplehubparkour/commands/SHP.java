package es.mithrandircraft.simplehubparkour.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SHP implements CommandExecutor {

    private final es.mithrandircraft.simplehubparkour.SimpleHubParkour mainClassAccess;

    public SHP(es.mithrandircraft.simplehubparkour.SimpleHubParkour main) { this.mainClassAccess = main; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) //Non parametrized command:
        {
            if (sender instanceof Player) //Is player
            {
                Player player = (Player) sender;
                if (args[0].equalsIgnoreCase("reload")) {
                    if (player.hasPermission("SPC.commands.Reload")) {
                        //Do reload
                        mainClassAccess.reloadConfig();
                        player.sendMessage("SimpleHubParkour has reloaded.");
                    } else player.sendMessage("You do not have permission to execute this command.");
                } else player.sendMessage("Invalid command argument.");
            } else { //Is console
                if (args[0].equalsIgnoreCase("reload")) {
                    //Do reload
                    mainClassAccess.reloadConfig();
                    System.out.println("SimpleHubParkour has reloaded.");
                } else System.out.println("Invalid command argument.");
            }
        }
        else if(args.length == 0)
        {
            if (sender instanceof Player) //Is player
            {
                Player player = (Player) sender;
                player.sendMessage("Please provide an argument.");
            }
            else //Is console
            {
                System.out.println("Please provide an argument.");
            }
        }
        else
        {
            if (sender instanceof Player) //Is player
            {
                Player player = (Player) sender;
                player.sendMessage("Invalid command argument.");
            }
            else //Is console
            {
                System.out.println("Invalid command argument.");
            }
        }
        return false;
    }
}

