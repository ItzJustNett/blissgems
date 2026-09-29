package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** /goldendream - admin controls for the Golden Dream. */
public final class GoldenDreamCommand implements TabExecutor {
    private static final String NEEDS_CITIZENS = "&cNPC memories need the &fCitizens &cplugin (not installed). Player memories work without it: &f/goldendream memory add <player>";
    private final GoldenDream dream;

    public GoldenDreamCommand(GoldenDream dream) {
        this.dream = dream;
    }

    private static void say(CommandSender s, String msg) {
        s.sendMessage(PedestalManager.color(msg));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            say(sender, "&cYou don't have permission.");
            return true;
        }
        if (args.length == 0) {
            this.help(sender);
            return true;
        }
        GoldenDreamWorld world = this.dream.world();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> this.give(sender, args);
            case "import" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                    say(sender, "&eThis copies the world folder at &f/" + GoldenDreamWorld.IMPORT_FOLDER + " &ein over the memory overworld (nether and end are left as-is); everyone in the dream is moved out first.");
                    say(sender, "&eType &f/goldendream import confirm &eto do it.");
                } else {
                    world.importOverworld(sender);
                }
            }
            case "reset" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                    say(sender, "&cThis deletes and regenerates the pocket, overworld, nether and end, and drops every dream record — a brand-new dream.");
                    say(sender, "&eType &f/goldendream reset confirm &eto do it.");
                } else {
                    world.resetAll(sender);
                }
            }
            case "walk" -> this.walk(sender, args);
            case "skip" -> {
                Player p = this.target(sender, args, 1);
                if (p == null) return true;
                if (world.skip(p.getUniqueId())) say(sender, "&6" + p.getName() + " &7leaves the pocket now.");
                else say(sender, "&c" + p.getName() + " is not walking the pocket.");
            }
            case "wake" -> {
                if (args.length < 2 && !(sender instanceof Player)) {
                    say(sender, "&cName a player: /goldendream wake <player>");
                    return true;
                }
                Player p = this.target(sender, args, 1);
                if (p == null) return true;
                if (world.wake(p)) say(sender, "&6" + p.getName() + " &7wakes from the dream.");
                else say(sender, "&c" + p.getName() + " is not in the dream.");
            }
            case "thunder" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("float")) {
                    int n = WakeFloat.start(this.dream.plugin(), this.dream);
                    if (n == 0) say(sender, "&cNobody in the dream to wake.");
                    else say(sender, "&6" + n + " &7in the dream drift up - kicked awake 20 blocks up.");
                    return true;
                }
                if (!(sender instanceof Player p)) {
                    say(sender, "&cIn-game only.");
                    return true;
                }
                PurgeThunder.strike(this.dream.plugin(), this.dream, p.getLocation());
                say(sender, "&6The great thunder &7is drawn past you - look up.");
            }
            case "memory" -> this.memory(sender, args);
            case "nick" -> this.nick(sender, args);
            default -> this.help(sender);
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        // /goldendream give <fragment <1-7|all>|core> [player]
        if (args.length < 2) {
            say(sender, "&eUsage: /goldendream give <fragment <1-7|all>|core> [player]");
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        int playerArg;
        if (args[1].equalsIgnoreCase("core")) {
            items.add(RitualItems.fragmentCore());
            playerArg = 2;
        } else if (args[1].equalsIgnoreCase("fragment") && args.length >= 3) {
            if (args[2].equalsIgnoreCase("all")) {
                for (int i = 1; i <= 7; i++) items.add(RitualItems.wireFragment(i));
            } else {
                try {
                    int n = Integer.parseInt(args[2]);
                    if (n < 1 || n > 7) throw new NumberFormatException();
                    items.add(RitualItems.wireFragment(n));
                } catch (NumberFormatException e) {
                    say(sender, "&cFragment number must be 1-7 or all.");
                    return;
                }
            }
            playerArg = 3;
        } else {
            say(sender, "&eUsage: /goldendream give <fragment <1-7|all>|core> [player]");
            return;
        }
        Player p = this.target(sender, args, playerArg);
        if (p == null) return;
        for (ItemStack left : p.getInventory().addItem(items.toArray(new ItemStack[0])).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
        say(sender, "&aGave &f" + items.size() + " &aritual item(s) to &f" + p.getName() + "&a.");
    }

    private void walk(CommandSender sender, String[] args) {
        if (args.length < 2) {
            say(sender, "&eUsage: /goldendream walk <start|stop|send> [player]");
            return;
        }
        GoldenDreamWorld world = this.dream.world();
        String mode = args[1].toLowerCase(Locale.ROOT);
        List<UUID> who = new ArrayList<>();
        if (args.length >= 3) {
            Player p = Bukkit.getPlayerExact(args[2]);
            if (p == null) {
                say(sender, "&cPlayer not found: " + args[2]);
                return;
            }
            who.add(p.getUniqueId());
        } else {
            who.addAll(world.walking());
        }
        if (who.isEmpty()) {
            say(sender, "&cNobody is walking the pocket.");
            return;
        }
        List<String> names = new ArrayList<>();
        for (UUID id : who) {
            boolean ok = switch (mode) {
                case "start" -> world.startWalk(id);
                case "stop" -> world.stopWalk(id);
                case "send" -> world.skip(id);
                default -> false;
            };
            Player p = Bukkit.getPlayer(id);
            if (ok && p != null) names.add(p.getName());
        }
        if (names.isEmpty()) {
            say(sender, mode.equals("start") || mode.equals("stop") || mode.equals("send") ? "&cNobody is walking the pocket." : "&eUsage: /goldendream walk <start|stop|send> [player]");
            return;
        }
        String verb = switch (mode) {
            case "start" -> "&7The walk starts: &6";
            case "stop" -> "&7The walk stops, free to move: &6";
            default -> "&7Into the memory now: &6";
        };
        say(sender, verb + String.join(", ", names));
    }

    private void memory(CommandSender sender, String[] args) {
        MemoryRoster roster = this.dream.roster();
        MemoryNpcs npcs = this.dream.npcs();
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "add" -> {
                if (args.length < 3) {
                    say(sender, "&eUsage: /goldendream memory add <player> [name]");
                    return;
                }
                Player p = Bukkit.getPlayerExact(args[2]);
                if (p == null) {
                    say(sender, "&cPlayer not found: " + args[2]);
                    return;
                }
                String name = roster.assign(p, args.length > 3 ? args[3] : null);
                if (name == null) say(sender, "&cNo free name in the pool" + (args.length > 3 ? " matching &f" + args[3] : "") + "&c.");
                else say(sender, "&6" + p.getName() + " &7is now the memory &6" + name + "&7.");
            }
            case "remove" -> {
                Player p = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : null;
                if (p == null || !roster.isMemory(p.getUniqueId())) {
                    say(sender, "&cNot a memory: " + (args.length > 2 ? args[2] : "?"));
                    return;
                }
                roster.remove(p);
                say(sender, "&6" + p.getName() + " &7is no longer a memory.");
            }
            case "name" -> {
                Player p = args.length > 3 ? Bukkit.getPlayerExact(args[2]) : null;
                if (p == null) {
                    say(sender, "&eUsage: /goldendream memory name <player> <newname>");
                    return;
                }
                if (roster.rename(p, args[3])) say(sender, "&7Memory renamed to &6" + args[3]);
                else say(sender, "&cNot a memory, or that name is taken.");
            }
            case "skin" -> {
                Player p = args.length > 3 ? Bukkit.getPlayerExact(args[2]) : null;
                if (p == null || !roster.isMemory(p.getUniqueId())) {
                    say(sender, "&eUsage: /goldendream memory skin <player> <source|off>");
                    return;
                }
                this.skin(sender, p, args[3]);
            }
            case "npc" -> {
                if (!npcs.available()) {
                    say(sender, NEEDS_CITIZENS);
                    return;
                }
                this.npc(sender, args);
            }
            default -> {
                say(sender, "&6Name pool: &f" + String.join(", ", roster.names()));
                List<String> active = new ArrayList<>();
                for (UUID id : roster.activeIds()) active.add(roster.nameOf(id));
                say(sender, "&6Player memories: &f" + (active.isEmpty() ? "none" : String.join(", ", active)));
                say(sender, npcs.available() ? "&6NPC memories: &f" + npcs.count() + (npcs.count() > 0 ? " (" + String.join(", ", npcs.liveNames()) + ")" : "") : "&7NPC memories: off (needs Citizens)");
            }
        }
    }

    private void npc(CommandSender sender, String[] args) {
        MemoryNpcs npcs = this.dream.npcs();
        if (args.length < 3) {
            say(sender, "&eUsage: /goldendream memory npc <count|clear|name|skin> ...");
            return;
        }
        String a = args[2].toLowerCase(Locale.ROOT);
        if (a.equals("clear")) {
            say(sender, "&7Cleared &6" + npcs.clearAll() + " &7NPC memories.");
        } else if (a.equals("name")) {
            if (args.length < 5) say(sender, "&eUsage: /goldendream memory npc name <bot> <newname>");
            else say(sender, npcs.rename(args[3], args[4]) ? "&7Bot renamed to &6" + args[4] : "&cNo such bot, or that name is taken.");
        } else if (a.equals("skin")) {
            if (args.length < 5) say(sender, "&eUsage: /goldendream memory npc skin <bot> <player|off>");
            else say(sender, npcs.reskin(args[3], args[4].equalsIgnoreCase("off") ? null : args[4]) ? "&7Bot skin set." : "&cNo such bot.");
        } else {
            if (!(sender instanceof Player p)) {
                say(sender, "&cIn-game only.");
                return;
            }
            int count;
            try {
                count = Math.max(1, Math.min(50, Integer.parseInt(a)));
            } catch (NumberFormatException e) {
                say(sender, "&eUsage: /goldendream memory npc <count|clear|name|skin> ...");
                return;
            }
            String skin = args.length > 3 ? args[3] : null;
            List<String> made = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String n = npcs.spawn(p.getLocation(), skin);
                if (n == null) break;
                made.add(n);
            }
            say(sender, made.isEmpty() ? "&cNo NPC spawned (no free names left?)." : "&7Spawned &6" + made.size() + " &7NPC memories: &f" + String.join(", ", made));
        }
    }

    private void nick(CommandSender sender, String[] args) {
        if (args.length < 3) {
            say(sender, "&eUsage: /goldendream nick <name|skin|reset> <player> [value|off]");
            return;
        }
        Player p = Bukkit.getPlayerExact(args[2]);
        if (p == null) {
            say(sender, "&cPlayer not found: " + args[2]);
            return;
        }
        NickManager nicks = this.dream.nicks();
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "name" -> {
                if (args.length < 4) {
                    say(sender, "&eUsage: /goldendream nick name <player> <text|off>");
                } else if (args[3].equalsIgnoreCase("off")) {
                    nicks.clearName(p);
                    say(sender, "&7Name back for &6" + p.getName());
                } else {
                    String text = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                    nicks.setName(p, text);
                    say(sender, "&7" + nicks.realName(p) + " now shows as &r" + text);
                }
            }
            case "skin" -> {
                if (args.length < 4) say(sender, "&eUsage: /goldendream nick skin <player> <source|off>");
                else this.skin(sender, p, args[3]);
            }
            case "reset" -> {
                nicks.reset(p);
                say(sender, "&7Real name and skin back for &6" + p.getName());
            }
            default -> say(sender, "&eUsage: /goldendream nick <name|skin|reset> <player> [value|off]");
        }
    }

    private void skin(CommandSender sender, Player p, String source) {
        NickManager nicks = this.dream.nicks();
        if (source.equalsIgnoreCase("off")) {
            nicks.clearSkin(p);
            say(sender, "&7Skin back for &6" + p.getName());
            return;
        }
        Player src = Bukkit.getPlayerExact(source);
        if (src == null) {
            say(sender, "&cThe skin source must be online: " + source);
            return;
        }
        if (nicks.setSkinFrom(p, src)) say(sender, "&6" + p.getName() + " &7now wears &6" + src.getName() + "&7's skin.");
        else say(sender, "&c" + src.getName() + " has no skin texture to borrow.");
    }

    private Player target(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player p = Bukkit.getPlayerExact(args[index]);
            if (p == null) say(sender, "&cPlayer not found: " + args[index]);
            return p;
        }
        if (sender instanceof Player p) return p;
        say(sender, "&cName a player.");
        return null;
    }

    private void help(CommandSender s) {
        say(s, "&6&lGolden Dream &7— commands");
        say(s, "&e/goldendream give <fragment <1-7|all>|core> [player] &7— ritual items");
        say(s, "&e/goldendream thunder &7— draw the great thunder across the sky past you");
        say(s, "&e/goldendream thunder float &7— everyone in the dream who is not a memory drifts up and is kicked awake");
        say(s, "&e/goldendream wake [player] &7— leave the dream for good; inventory and place restored");
        say(s, "&e/goldendream skip [player] &7— end the pocket walk now, into the memory");
        say(s, "&e/goldendream walk <start|stop|send> [player] &7— control the golden-sky walk");
        say(s, "&e/goldendream memory add|remove|name|skin|list ... &7— player memories");
        say(s, "&e/goldendream memory npc <count|clear|name|skin> &7— bot memories " + (this.dream.npcs().available() ? "" : "&c(needs Citizens)"));
        say(s, "&e/goldendream nick <name|skin|reset> <player> ... &7— fake names and skins");
        say(s, "&e/goldendream import confirm &7— copy &f/" + GoldenDreamWorld.IMPORT_FOLDER + " &7in as the memory overworld");
        say(s, "&e/goldendream reset confirm &7— a brand-new dream: fresh worlds, every record dropped");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) return List.of();
        List<String> opts = switch (args.length) {
            case 1 -> List.of("give", "thunder", "wake", "skip", "walk", "memory", "nick", "import", "reset");
            case 2 -> switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give" -> List.of("fragment", "core");
                case "thunder" -> List.of("float");
                case "walk" -> List.of("start", "stop", "send");
                case "memory" -> List.of("add", "remove", "name", "skin", "list", "npc");
                case "nick" -> List.of("name", "skin", "reset");
                case "import", "reset" -> List.of("confirm");
                case "wake", "skip" -> players();
                default -> List.of();
            };
            case 3 -> switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give" -> args[1].equalsIgnoreCase("fragment") ? List.of("1", "2", "3", "4", "5", "6", "7", "all") : players();
                case "memory" -> args[1].equalsIgnoreCase("npc") ? List.of("clear", "name", "skin", "1", "5", "10") : players();
                case "walk", "nick" -> players();
                default -> List.of();
            };
            case 4 -> args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("nick") || args[0].equalsIgnoreCase("memory") ? Stream.concat(players().stream(), Stream.of("off")).toList() : List.of();
            default -> List.of();
        };
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return opts.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
    }

    private static List<String> players() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }
}
