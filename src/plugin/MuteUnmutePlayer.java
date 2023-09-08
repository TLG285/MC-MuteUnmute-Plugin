package plugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuteUnmutePlayer extends JavaPlugin implements CommandExecutor, Listener {

    private final Map<UUID, MuteEntry> mutedPlayers = new HashMap<>();
    private File mutedPlayersFile;
    private final Yaml yaml = new Yaml(new DumperOptions());

    @Override
    public void onEnable() {
        getCommand("mute").setExecutor(this);
        getCommand("unmute").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        // Verifică și creează directorul și fișierul dacă nu există
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();
        mutedPlayersFile = new File(getDataFolder(), "MutedPlayers.yml");

        // Apelează metoda saveMutesToYaml() la fiecare 6000 de tick-uri (5 minute)
        Bukkit.getScheduler().runTaskTimer(this, this::saveMutesToYaml, 0, 6000);

        loadMutedPlayers();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mute")) {
            if (args.length < 2) {
                sender.sendMessage("Utilizare corectă: /mute <Player> <Reason>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("Jucătorul nu este online.");
                return true;
            }
            UUID targetUUID = target.getUniqueId();
            if (mutedPlayers.containsKey(targetUUID)) {
                sender.sendMessage("Acest jucător a primit deja mute.");
                return true;
            }
            StringBuilder reason = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reason.append(args[i]).append(" ");
            }
            MuteEntry muteEntry = new MuteEntry(sender.getName(), reason.toString().trim());
            mutedPlayers.put(targetUUID, muteEntry);
            sender.sendMessage(target.getName() + " a primit mute cu succes pentru motivul: " + reason);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("unmute")) {
            if (args.length != 1) {
                sender.sendMessage("Utilizare corectă: /unmute <Player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("Jucătorul nu este online.");
                return true;
            }
            UUID targetUUID = target.getUniqueId();
            if (!mutedPlayers.containsKey(targetUUID)) {
                sender.sendMessage("Acest jucător nu a primit mute.");
                return true;
            }
            mutedPlayers.remove(targetUUID);
            sender.sendMessage(target.getName() + " a primit unmute cu succes.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (mutedPlayers.containsKey(playerUUID)) {
            MuteEntry muteEntry = mutedPlayers.get(playerUUID);
            player.sendMessage("Ai primit mute de la " + muteEntry.getStaff() + " pentru motivul: " + muteEntry.getReason());
            event.setCancelled(true);
        }
    }

    private void saveMutesToYaml() {
        Map<String, Map<String, String>> mutedPlayersData = new HashMap<>();
        for (Map.Entry<UUID, MuteEntry> entry : mutedPlayers.entrySet()) {
            String playerUUID = entry.getKey().toString();
            MuteEntry muteEntry = entry.getValue();
            Map<String, String> muteData = new HashMap<>();
            muteData.put("staff", muteEntry.getStaff());
            muteData.put("reason", muteEntry.getReason());
            mutedPlayersData.put(playerUUID, muteData);
        }

        try (OutputStream outputStream = new FileOutputStream(mutedPlayersFile)) {
            yaml.dump(mutedPlayersData, new OutputStreamWriter(outputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMutedPlayers() {
        if (mutedPlayersFile.exists()) {
            try (InputStream inputStream = new FileInputStream(mutedPlayersFile)) {
                Map<String, Map<String, String>> mutedPlayersData = yaml.load(inputStream);

                if (mutedPlayersData != null) {
                    for (Map.Entry<String, Map<String, String>> entry : mutedPlayersData.entrySet()) {
                        UUID playerUUID = UUID.fromString(entry.getKey());
                        Map<String, String> muteData = entry.getValue();
                        String staff = muteData.get("staff");
                        String reason = muteData.get("reason");

                        MuteEntry muteEntry = new MuteEntry(staff, reason);
                        mutedPlayers.put(playerUUID, muteEntry);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class MuteEntry {
        private final String staff;
        private final String reason;

        public MuteEntry(String staff, String reason) {
            this.staff = staff;
            this.reason = reason;
        }

        public String getStaff() {
            return staff;
        }

        public String getReason() {
            return reason;
        }
    }

    @Override
    public void onDisable() {
        saveMutesToYaml();
    }
}
