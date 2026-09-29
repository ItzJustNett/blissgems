package dev.xoperr.blissgems.villagerevent;

import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

/** /blissevent start|end|phase|compass|villager|give */
public final class VillagerEventCommand implements TabExecutor {
    private final VillagerEventManager manager;

    public VillagerEventCommand(VillagerEventManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(PedestalManager.color("&cYou don't have permission."));
            return true;
        }
        if (args.length == 0) {
            this.usage(sender);
            return true;
        }
        VillagerEventState state = this.manager.state();
        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (state.compassLoc() == null) {
                    sender.sendMessage(PedestalManager.color("&cSet compass first: /blissevent compass set"));
                    return true;
                }
                this.phase1(sender);
                this.manager.phase2();
            }
            case "phase" -> {
                String p = args.length < 2 ? "" : args[1];
                switch (p) {
                    case "1" -> this.phase1(sender);
                    case "2" -> {
                        if (state.compassLoc() == null) sender.sendMessage(PedestalManager.color("&cSet compass first: /blissevent compass set"));
                        else this.manager.phase2();
                    }
                    case "3" -> this.manager.phase3();
                    default -> sender.sendMessage(PedestalManager.color("&7/blissevent phase <1|2|3>"));
                }
            }
            case "end" -> {
                if (!this.manager.end()) sender.sendMessage(PedestalManager.color("&cNo event is running."));
            }
            case "compass" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PedestalManager.color("&cPlayer only."));
                    return true;
                }
                if (args.length < 2 || !args[1].equalsIgnoreCase("set")) {
                    this.usage(sender);
                    return true;
                }
                if (!player.getLocation().clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                    sender.sendMessage(PedestalManager.color("&cStand on a solid block first — the compass can't be set in mid-air."));
                    return true;
                }
                state.setCompassLoc(player.getLocation());
                state.save();
                sender.sendMessage(PedestalManager.color("&aVillage compass set to your location."));
            }
            case "villager" -> this.handleVillager(sender, args);
            case "give" -> this.handleGive(sender, args);
            default -> this.usage(sender);
        }
        return true;
    }

    private void phase1(CommandSender sender) {
        if (DiscSummonRitual.isRunning()) {
            sender.sendMessage(PedestalManager.color("&eThe disc ritual is already running."));
        } else if (!this.manager.phase1()) {
            sender.sendMessage(PedestalManager.color("&eNo pedestal location is set — disc ritual skipped. Set one with /bliss pedestal set first."));
        }
    }

    private void handleVillager(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PedestalManager.color("&cPlayer only."));
            return;
        }
        Integer id = args.length >= 3 ? parseId(args[2]) : null;
        if (id == null || !(args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("restock"))) {
            sender.sendMessage(PedestalManager.color("&7/blissevent villager <set|restock> <1|2|3>"));
            return;
        }
        VillagerEventState state = this.manager.state();
        if (args[1].equalsIgnoreCase("restock")) {
            if (!state.isTradeBoughtAny(id)) {
                sender.sendMessage(PedestalManager.color("&7Villager &f" + id + "&7 has nothing to restock — its one-time trades are unclaimed."));
                return;
            }
            state.clearTradeBought(id);
            Villager v = state.getVillager(id);
            if (v != null && v.isValid()) this.manager.items().applyTrades(id, v);
            state.save();
            sender.sendMessage(PedestalManager.color("&aVillager &f" + id + "&a restocked — its one-time trades can be bought again."
                + (v != null && v.isValid() ? "" : " &7(the villager itself isn't loaded; the rows refresh when it is)")));
            return;
        }
        Villager target = null;
        for (Entity e : player.getNearbyEntities(10, 10, 10)) {
            if (e instanceof Villager v) {
                target = v;
                break;
            }
        }
        if (target == null) {
            sender.sendMessage(PedestalManager.color("&cNo villager found within 10 blocks."));
            return;
        }
        target.setInvulnerable(false);
        this.manager.items().dressVillager(target, id);
        state.setVillager(id, target);
        state.save();
        sender.sendMessage(PedestalManager.color("&aThat villager is now village &f" + id + "&a (" + VillagerEventItems.ownerName(id) + ")."));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PedestalManager.color("&cPlayer only."));
            return;
        }
        ItemStack item = null;
        if (args.length >= 2 && args[1].equalsIgnoreCase("token")) {
            item = this.manager.items().energyToken();
        } else if (args.length >= 3 && args[1].equalsIgnoreCase("soul") && parseId(args[2]) != null) {
            item = this.manager.items().soul(parseId(args[2]));
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("disc")) {
            item = this.manager.items().villageDisc();
        }
        if (item == null) {
            this.usage(sender);
            return;
        }
        for (ItemStack left : player.getInventory().addItem(item).values()) player.getWorld().dropItemNaturally(player.getLocation(), left);
    }

    private static Integer parseId(String s) {
        return switch (s) {
            case "1" -> 1;
            case "2" -> 2;
            case "3" -> 3;
            default -> null;
        };
    }

    private void usage(CommandSender sender) {
        String[] lines = {
            "&6/blissevent &7— Run the Villager event.",
            "&e start &7Begin the event (phase-1 disc ritual + soul hunt)",
            "&e end &7End the event",
            "&e phase <1|2|3> &7Jump straight to a phase",
            "&e compass set &7Point the event compass here",
            "&e villager set <1|2|3> &7Make the nearest villager a village's villager",
            "&e villager restock <1|2|3> &7Put its one-time trades back on sale",
            "&e give <token|disc|soul <1|2|3>> &7Hand out event items"};
        for (String l : lines) sender.sendMessage(PedestalManager.color(l));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = switch (args.length) {
            case 1 -> List.of("start", "end", "phase", "compass", "villager", "give");
            case 2 -> switch (args[0].toLowerCase()) {
                case "phase" -> List.of("1", "2", "3");
                case "compass" -> List.of("set");
                case "villager" -> List.of("set", "restock");
                case "give" -> List.of("token", "disc", "soul");
                default -> List.<String>of();
            };
            case 3 -> args[0].equalsIgnoreCase("villager") || args[1].equalsIgnoreCase("soul") ? List.of("1", "2", "3") : List.<String>of();
            default -> List.<String>of();
        };
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.startsWith(args[args.length - 1].toLowerCase())) out.add(o);
        return out;
    }
}
