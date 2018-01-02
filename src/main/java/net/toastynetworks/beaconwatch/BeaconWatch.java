package net.toastynetworks.beaconwatch;

import com.flowpowered.math.vector.Vector3d;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldArchetypes;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * @author Brend
 *
 */
/**
 * @author Brend
 *
 */
@Plugin(id = "beaconwatch", name = "BeaconWatch", version = "1.0")
public class BeaconWatch {

    @Inject
    Game game;
    @Inject
    private Logger logger;
    @Inject
    private PluginContainer container;
    public static BeaconWatch instance;
    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path defaultConfig;
    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;
    private ConfigurationNode config;
    public long fullGameTime;
    public long resourceTime;
    public int minPlayersGameStart;
    public int resourceProtectionRadius;
    public String arenaWorldName;
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;
    public int playersPerTeam;
    public int teamCount;
    Random random = new Random();
    GamePhase phase = GamePhase.PREGAME;
    Map<TextColor, Team> teams = new HashMap<>();

    @Listener
    public void onInit(GameInitializationEvent event) {
        instance = this;
        try {
            this.config = this.configManager.load();
            if (!this.defaultConfig.toFile().exists()) {
                this.config.getNode("FullGameTime").setValue(1200000);
                this.config.getNode("ResourceTime").setValue(600000);
                this.config.getNode("MinPlayersGameStart").setValue(8);
                this.config.getNode("TeamCount").setValue(2);
                this.config.getNode("PlayersPerTeam").setValue(1);
                this.config.getNode("ResourceProtectionRadius").setValue(75);
                this.config.getNode("ArenaWorldName").setValue("Arena");
                this.config.getNode("MinX").setValue(20);
                this.config.getNode("MinY").setValue(64);
                this.config.getNode("MinZ").setValue(20);
                this.config.getNode("MaxX").setValue(700);
                this.config.getNode("MaxY").setValue(81);
                this.config.getNode("MaxZ").setValue(700);
                this.configManager.save(this.config);
            }
            this.fullGameTime = this.config.getNode("FullGameTime").getLong();
            this.resourceTime = this.config.getNode("ResourceTime").getLong();
            this.minPlayersGameStart = this.config.getNode("MinPlayersGameStart").getInt();
            this.resourceProtectionRadius = (int) Math.pow(this.config.getNode("ResourceProtectionRadius").getInt(), 2);
            this.arenaWorldName = this.config.getNode("ArenaWorldName").getString();
            this.teamCount = this.config.getNode("TeamCount").getInt();
            this.playersPerTeam = this.config.getNode("PlayersPerTeam").getInt();
            this.minX = this.config.getNode("MinX").getInt();
            this.minY = this.config.getNode("MinY").getInt();
            this.minZ = this.config.getNode("MinZ").getInt();
            this.maxX = this.config.getNode("MaxX").getInt();
            this.maxY = this.config.getNode("MaxY").getInt();
            this.maxZ = this.config.getNode("MaxZ").getInt();
            this.logger.info("Full game time is set to: " + this.fullGameTime);
            this.logger.info("Resource phase time set to: " + this.resourceTime);
            this.logger.info("Min amount of players required to start game is set to: " + this.minPlayersGameStart);
            this.logger.info("Protection radius for resource phase is set to: " + this.resourceProtectionRadius);
            this.logger.info("Arena world is set to: " + this.arenaWorldName);
            this.logger.info("------------------------------------");
            this.logger.info("ToastyWalls was successfully loaded!");
            this.logger.info("------------------------------------");
        } catch (IOException e) {
            this.logger.warn("Error loading and or creating default configuration!");
            this.logger.warn("-------------------------------------------------------------");
            this.logger.warn("ToastyWalls was not loaded correctly due to a config error");
            this.logger.warn("-------------------------------------------------------------");
            e.printStackTrace();
        }
    }

    @Listener
    public void userLogin(ClientConnectionEvent.Join event) {
        if (this.phase == GamePhase.PREGAME) {
            Location<World> location = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get().getSpawnLocation();
            Player player = event.getTargetEntity();
            player.setLocation(location);
            this.logger.info("Location of spawn is set to: " + location);
            if (Sponge.getServer().getOnlinePlayers().size() >= this.minPlayersGameStart) {
                this.phase = GamePhase.RESOURCE;
                this.logger.info("Resource phase starting");

                Map<UUID, TeamMember> members = new HashMap<>();
                Collection<TextColor> colors = Sponge.getRegistry().getAllOf(TextColor.class);
                Iterator<TextColor> colorsIterator = colors.iterator();
                for (Player p : Sponge.getServer().getOnlinePlayers()) {
                    members.put(p.getUniqueId(), new TeamMember(p.getUniqueId()));
                    if (members.size() == this.playersPerTeam) {
                        TextColor teamColor = colorsIterator.next();
                        Team team = new Team(teamColor, members, this.calculateRandomBeacon());
                        this.teams.put(teamColor, team);
                        this.placeBeacon(team.getBeacon());
                        this.logger.info("New team: " + team.toString());
                        for (TeamMember member : members.values()) {
                            Player teamPlayer = member.getPlayer().get();
                            teamPlayer.setLocation(team.getBeacon());
                            this.logger.info(teamPlayer + " was sent to beacon location: " + team.getBeacon());
                        }
                        members = new HashMap<>();
                    }
                }
                Task.builder().execute(() -> {
                    this.phase = GamePhase.PVP;
                    this.logger.info("Resource phase is over!");
                    this.logger.info("PVP phase has began!");
                }).delay(resourceTime, TimeUnit.MILLISECONDS).submit(this);
            }
        }
    }

    public void deleteArena() {
        Optional<World> world = Sponge.getServer().getWorld(this.arenaWorldName);
        if (world.isPresent()) {
            this.logger.info("Deleting arena world...");
            Sponge.getServer().unloadWorld(world.get());
            Sponge.getServer().deleteWorld(this.getArenaWorldProperties());
        } else {
            this.logger.info("World does not exist, so it can't be removed.");
        }
    }

    public void createArena() {
        try {
            this.logger.info("Creating arena world...");
            WorldProperties properties = Sponge.getServer().createWorldProperties(this.arenaWorldName, WorldArchetypes.OVERWORLD);
            if (Sponge.getServer().getWorld(this.arenaWorldName).isPresent()) {
                this.logger.info("Seems like " + this.arenaWorldName + " is loaded!");
            } else {
                this.logger.info(this.arenaWorldName + " was not loaded.....");
                this.logger.info("Loading now...");
                Sponge.getServer().loadWorld(properties);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WorldProperties getArenaWorldProperties() {
        return Sponge.getServer().getWorldProperties(this.arenaWorldName).get();
    }

    public void placeBeacon(Location<World> blockloc) {
        blockloc.setBlockType(BlockTypes.BEACON, Cause.source(container).build());
    }

    public int calculateBeaconY(int x, int z) {
        this.logger.info("Printing debug message: - before " + "yMin: " + this.minY);
        this.logger.info("Printing debug message: - before " + "yMax: " + this.maxY);
        //this.logger.info("Printing debug message: - before " + "y: " + y);
        World world = Sponge.getServer().getWorld(this.arenaWorldName).get();

        for (int y = this.maxY; y >= this.minY; y--) {
            Location<World> loc = world.getLocation(x, y, z);
            if (loc.getBlockType().equals(BlockTypes.AIR)) {
                this.logger.info("Block type: " + loc.getBlockType().getName());
                this.logger.info("Passed check 1 (air)");
            } else if (loc.getBlockType().equals(BlockTypes.GRASS) || loc.getBlockType().equals(BlockTypes.STONE) || loc.getBlockType().equals(BlockTypes.SAND) || loc.getBlockType().equals(BlockTypes.COBBLESTONE)) {
                this.logger.info("Passed check 2 (solid block)");
                if (loc.add(0, 1, 0).getBlockType().equals(BlockTypes.AIR)) {
                    this.logger.info("Passed check 3 (Air above solid block)");
                    this.logger.info("Found a good location! " + "Coordinate Y: " + (y + 1));
                    return y + 1;
                } else {
                    this.logger.info("Passed check 4 (if no air above solid block)");
                }
            } else {
                return -1;
            }
        }

        return -1;
    }

    /**
     * Calculates random beacon location
     * @return valid location of beacon, null if not found
     */
    public Location<World> calculateRandomBeacon() {
        World world = Sponge.getServer().getWorld(this.arenaWorldName).get();
        for (int attempts = 0; attempts < 10000; attempts++) {
            int x = this.random.nextInt(this.maxX - this.minX) + this.minX;
            int z = this.random.nextInt(this.maxZ - this.minZ) + this.minZ;

            int y = calculateBeaconY(x, z);
            if (y != -1) {
                this.logger.info("Final beacon destination: " + world.getLocation(x, y, z));
                return world.getLocation(x, y, z);
            } else {
                this.logger.info("y = -1, recalculating...");
            }
        }
        this.logger.info("We fucked up");
        return null;
    }


    /**
     * Get the team for the specified player's UUID
     * 
     * @param uuid the UUID for the player
     * @return the Team for the player, or null if not found
     */
    public Team getTeam(UUID uuid) {
        //Loop through all teams and get values
        for (Team team : this.teams.values()) {
            //Check if specified player is in a team
            if (team.getPlayers().containsKey(uuid)) {
                return team;
            }
        }
        return null;
    }

    @Listener
    public void afterInit(GameStartedServerEvent event) {
        this.createArena();
    }

    @Listener
    public void gameStop(GameStoppingEvent event) {
        this.deleteArena();
    }

    @Listener
    public void onDamageEntity(DamageEntityEvent event, @First Player source) {
        if (this.phase == GamePhase.RESOURCE) {
            if (event.getTargetEntity() instanceof Player) {
                event.setCancelled(true);
            }
        } else if (this.phase == GamePhase.PVP) {
            if (event.getTargetEntity() instanceof Player) {
                Player targetplayer = (Player) event.getTargetEntity();
                //Find out if the source of the damage is in the same team of the target(player)
                Team sourceTeam = this.getTeam(source.getUniqueId());
                if (sourceTeam.getPlayers().containsKey(targetplayer.getUniqueId())) {
                    event.setCancelled(true);
                    source.sendMessage(Text.of(TextColors.RED, "Fight the enemy, not your team, fool!"));
                }
            }
        }
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event, @First Player source) {
        if (this.phase == GamePhase.RESOURCE) {
            for (Transaction<BlockSnapshot> trans : event.getTransactions()) {
                BlockSnapshot blockSnapshot = trans.getOriginal();
                BlockType blockType = blockSnapshot.getState().getType();
                if (blockType == BlockTypes.BEACON) {
                    event.setCancelled(true);
                    trans.setValid(false);
                }
            }
        } else if (this.phase == GamePhase.PVP) {
            for (Transaction<BlockSnapshot> trans : event.getTransactions()) {
                BlockSnapshot blockSnapshot = trans.getOriginal();
                if (blockSnapshot.getLocation().isPresent()) {
                    BlockType blockType = blockSnapshot.getState().getType();
                    if (blockType == BlockTypes.BEACON) {
                        //Find out if the beacon that source is breaking is from their own team
                        Team sourceTeam = this.getTeam(source.getUniqueId());
                        if (sourceTeam.getBeacon().equals(blockSnapshot.getLocation().get())) {
                            event.setCancelled(true);
                            trans.setValid(false);
                            source.sendMessage(Text.of(TextColors.RED, "Stop trying to destroy your own beacon.."));
                        }
                    }
                }
            }
        }
    }

    @Listener
    public void onEntityMovement(MoveEntityEvent event, @First Player source) {
        Vector3d location = event.getFromTransform().getPosition();
        for (Team team : this.teams.values()) {
            if (!team.getPlayers().containsKey(source.getUniqueId())) {
                if (location.distanceSquared(team.getBeacon().getPosition()) < this.resourceProtectionRadius) {
                    PotionEffect blindness = PotionEffect.builder().potionType(PotionEffectTypes.BLINDNESS).duration(20).build();
                    PotionEffect wither = PotionEffect.builder().potionType(PotionEffectTypes.WITHER).duration(20).build();
                    PotionEffect mining = PotionEffect.builder().potionType(PotionEffectTypes.MINING_FATIGUE).duration(20).amplifier(10).build();
                    PotionEffectData effects = source.getOrCreate(PotionEffectData.class).get();
                    effects.addElement(blindness);
                    effects.addElement(wither);
                    effects.addElement(mining);
                    source.offer(effects);
                    break;
                }
            }
        }
    }

    @Listener
    public void onPlayerRespawn(RespawnPlayerEvent event) {
        Player player = event.getOriginalPlayer();
        for (Team team : this.teams.values()) {
            if (team.getPlayers().containsKey(player.getUniqueId())) {
                event.setToTransform(event.getToTransform().setLocation(team.getBeacon()));
                break;
            }
        }
    }
}