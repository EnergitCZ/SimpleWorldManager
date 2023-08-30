package dev.energit.SimpleWorldManager

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World.Environment
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

lateinit var logger: Logger

private fun registerLogger(pluginLogger: Logger) {
    logger = pluginLogger
}

class WorldConfig (
    val name: String,
    val environment: Environment,
    val seed: Long,
    var portalNether: String,
    var portalEnd: String
)

val worldsConfig = mutableMapOf<String, WorldConfig>()

val worldNames = mutableListOf<String>()

val forceLoad = mutableListOf<String>()

var enablePortalLinking = true
var enableSpawnOverride = true

lateinit var globalSpawn: Location

class SimpleWorldManager : JavaPlugin() {
    fun updateConfig(lastRevision: Int) {
        when (lastRevision) {
            1 -> {
                this.config.set("override-spawn", true)
                this.config.set("global-spawn.world", "world")
                this.config.set("global-spawn.xyz", "default")
                this.config.set("config-version", 2)
            }
        }
    }

    override fun onEnable() {
        registerLogger(logger)

        this.saveDefaultConfig()

        updateConfig(this.config.getInt("config-version"))

        this.config.getStringList("world-names").forEach {
            worldNames.add(it)

            val wc = WorldConfig(
                it,
                when (this.config.getString("worlds.$it.type")) {
                    "NORMAL" -> Environment.NORMAL
                    "NETHER" -> Environment.NETHER
                    "THE_END" -> Environment.THE_END
                    else -> Environment.NORMAL
                },
                this.config.getLong("worlds.$it.seed"),
                this.config.getString("worlds.$it.portal-nether") ?: "world_nether",
                this.config.getString("worlds.$it.portal-end") ?: "world_the_end"
            )
            worldsConfig[it] = wc
        }

        this.config.getStringList("force-load").forEach {
            forceLoad.add(it)

            val world = Bukkit.getWorld(it)
            if (world == null) {
                simpleWorldManagerApi.loadWorld(it)
            } else {
                logger.info("World $it already loaded")
            }
        }

        enablePortalLinking = config.getBoolean("enable-portal-linking")

        if (enablePortalLinking) {
            Bukkit.getServer().pluginManager.registerEvents(PortalLinkListener(), this)
        }

        enableSpawnOverride = this.config.getBoolean("override-spawn")

        val spawnWorld = this.config.getString("global-spawn.world")!!

        if (!simpleWorldManagerApi.checkWorldLoaded(spawnWorld)) {
            simpleWorldManagerApi.loadWorld(spawnWorld)
        }

        if (this.config.isList("global-spawn.xyz")) {
            val xyz = this.config.getDoubleList("global-spawn.xyz")
            globalSpawn = Location(Bukkit.getWorld(spawnWorld), xyz[0], xyz[1], xyz[2], xyz[3].toFloat(), xyz[4].toFloat())
        } else {
            globalSpawn = Bukkit.getWorld(spawnWorld)!!.spawnLocation
        }

        if (enableSpawnOverride) {
            Bukkit.getServer().pluginManager.registerEvents(PlayerJoinListener(), this)
        }

        this.getCommand("swm")!!.setExecutor(SwmCommand())
    }

    override fun onDisable() {
        worldsConfig.forEach {
            val wc = it.value
            val path = "worlds.${wc.name}"
            this.config.set("$path.name", wc.name)
            this.config.set("$path.type", wc.environment.name)
            this.config.set("$path.seed", wc.seed)
            this.config.set("$path.portal-nether", wc.portalNether)
            this.config.set("$path.portal-end", wc.portalEnd)
        }

        this.config.set("world-names", worldNames)

        this.config.set("force-load", forceLoad)

        this.config.set("override-spawn", enableSpawnOverride)
        this.config.set("global-spawn.world", globalSpawn.world!!.name)
        this.config.set("global-spawn.xyz", arrayOf(globalSpawn.x, globalSpawn.y, globalSpawn.z, globalSpawn.yaw, globalSpawn.pitch))

        this.saveConfig()
    }

    fun getSimpleWorldManagerApi() : SimpleWorldManagerApi {
        return simpleWorldManagerApi
    }
}

private class PortalLinkListener : Listener {
    @EventHandler
    fun onNetherPortalEvent(ev: PlayerPortalEvent) {
        if (ev.to!!.world!!.environment == Environment.NETHER || ev.to!!.world!!.environment == Environment.NORMAL) {
            val sourceWorldName = ev.from.world!!.name
            if (sourceWorldName in arrayOf("world", "world_nether", "world_the_end")) {
                return
            }
            val destWorldName = worldsConfig[sourceWorldName]!!.portalNether
            var destWorld = Bukkit.getWorld(destWorldName)
            if (destWorld == null) {
                simpleWorldManagerApi.loadWorld(destWorldName)
                destWorld = Bukkit.getWorld(destWorldName)
                if (destWorld == null) {
                    logger.warning("Linking error on world $sourceWorldName")
                    destWorld = Bukkit.getWorld("world_nether")!!
                }
            }
            ev.setTo(destWorld.spawnLocation)
        }
    }

    @EventHandler
    fun onChangeWorld(ev: PlayerChangedWorldEvent) {
        if (ev.from.environment == Environment.THE_END) {
            val worldName = ev.from.name
            val destWorldName = worldsConfig[worldName]!!.portalEnd
            var destWorld = Bukkit.getWorld(destWorldName)
            if (destWorld == null) {
                simpleWorldManagerApi.loadWorld(destWorldName)
                destWorld = Bukkit.getWorld(destWorldName)
                if (destWorld == null) {
                    logger.warning("Linking error on world $worldName")
                    destWorld = Bukkit.getWorld("world_nether")!!
                }
            }
            ev.player.teleport(destWorld.spawnLocation)
        }
    }

    @EventHandler
    fun onTeleport(ev: PlayerTeleportEvent) {
        if (ev.cause == TeleportCause.END_PORTAL) {
            val sourceWorldName = ev.from.world!!.name
            if (sourceWorldName in arrayOf("world", "world_nether", "world_the_end")) {
                return
            }
            val destWorldName = worldsConfig[sourceWorldName]!!.portalEnd
            var destWorld = Bukkit.getWorld(destWorldName)
            if (destWorld == null) {
                simpleWorldManagerApi.loadWorld(destWorldName)
                destWorld = Bukkit.getWorld(destWorldName)
                if (destWorld == null) {
                    logger.warning("Linking error on world $sourceWorldName")
                    destWorld = Bukkit.getWorld("world_the_end")!!
                }
            }

            val x = 20
            val z = 0
            val y = destWorld.getHighestBlockYAt(x, z)
            val loc = Location(destWorld, x.toDouble(), y + 1.0, z.toDouble())

            ev.setTo(loc)
        }
    }
}

private class PlayerJoinListener : Listener {
    @EventHandler
    fun onJoin(ev: PlayerJoinEvent) {
        ev.player.teleport(globalSpawn)
    }
}

private class SwmCommand() : CommandExecutor {
    fun createWorld(sender: CommandSender, name: String, seed: Long?, environment: Environment?) {
        sender.sendMessage("Creating world")

        var res: CreateWorldResponse? = null

        if (seed == null && environment == null) {
            res = simpleWorldManagerApi.createWorld(name)
        } else if (seed == null && environment != null) {
            res = simpleWorldManagerApi.createWorld(name, environment)
        } else if (seed != null && environment == null) {
            res = simpleWorldManagerApi.createWorld(name, seed)
        } else if (seed != null && environment != null) {
            res = simpleWorldManagerApi.createWorld(name, seed, environment)
        } else {
            sender.sendMessage("How did this happen?")
        }

        when (res) {
            CreateWorldResponse.CREATION_ERROR -> { sender.sendMessage("An unknown error occurred while creating the world") }
            CreateWorldResponse.WORLD_EXISTS -> { sender.sendMessage("A world with that name already exists") }
            CreateWorldResponse.SUCCESS -> { sender.sendMessage("World created") }
            else -> { sender.sendMessage("An unknown error occurred") }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size > 1) {
            when (args[0]) {
                "create" -> {
                    when (args.size - 1) {
                        1 -> { createWorld(sender, args[1], null, null) }
                        2 -> {
                            val seed = args[2].toLongOrNull()
                            if (seed == null) {
                                val environment = when (args[2]) {
                                    "NORMAL" -> Environment.NORMAL
                                    "NETHER" -> Environment.NETHER
                                    "THE_END" -> Environment.THE_END
                                    else -> null
                                }
                                if (environment == null) {
                                    sender.sendMessage("Seeds can only be numeric")
                                } else {
                                    createWorld(sender, args[1], null, environment)
                                }
                            } else {
                                createWorld(sender, args[1], seed, null)
                            }
                        }
                        3 -> {
                            val seed = args[2].toLongOrNull()
                            val generator = when (args[3]) {
                                "NORMAL" -> Environment.NORMAL
                                "NETHER" -> Environment.NETHER
                                "THE_END" -> Environment.THE_END
                                else -> null
                            }
                            if (seed == null) {
                                sender.sendMessage("Seeds can only be numeric")
                            } else if (generator == null) {
                                sender.sendMessage("Generator with this name doesn't exist")
                            } else {
                                createWorld(sender, args[1], seed, generator)
                            }
                        }
                        else -> {
                            sender.sendMessage("Invalid usage")
                            return false
                        }
                    }
                }
                "tp", "teleport" -> {
                    when (args.size - 1) {
                        1 -> {
                            if (sender !is Player) {
                                sender.sendMessage("This command can be run only as a player")
                            } else {
                                when (simpleWorldManagerApi.teleportPlayerIntoWorld(sender, args[1])) {
                                    TeleportToWorldResponse.UNLOADED_WORLD -> { sender.sendMessage("Destination world isn't loaded") }
                                    TeleportToWorldResponse.NONEXISTENT_WORLD -> { sender.sendMessage("Destination world doesn't exist") }
                                    TeleportToWorldResponse.SUCCESS -> {}
                                }
                            }
                        }
                        2 -> {
                            val player = Bukkit.getPlayerExact(args[1])
                            if (player == null) {
                                sender.sendMessage("Specified player doesn't exist")
                            } else {
                                when (simpleWorldManagerApi.teleportPlayerIntoWorld(player, args[2])) {
                                    TeleportToWorldResponse.UNLOADED_WORLD -> { sender.sendMessage("Destination world isn't loaded") }
                                    TeleportToWorldResponse.NONEXISTENT_WORLD -> { sender.sendMessage("Destination world doesn't exist") }
                                    TeleportToWorldResponse.SUCCESS -> {}
                                }
                            }
                        }
                        else -> {
                            sender.sendMessage("Invalid usage")
                            return false
                        }
                    }
                }
                "load" -> {
                    if (args.size != 2) {
                        sender.sendMessage("Invalid usage")
                        return false
                    }
                    sender.sendMessage("Loading world")
                    when (simpleWorldManagerApi.loadWorld(args[1])) {
                        LoadWorldResponse.NONEXISTENT_WORLD -> { sender.sendMessage("A world with that name doesn't exist") }
                        LoadWorldResponse.LOAD_ERROR -> { sender.sendMessage("An error occurred while loading the world") }
                        LoadWorldResponse.SUCCESS -> { sender.sendMessage("World loaded") }
                    }
                }
                "unload" -> {
                    if (args.size != 2) {
                        sender.sendMessage("Invalid usage")
                        return false
                    }
                    sender.sendMessage("Unloading world")
                    if (simpleWorldManagerApi.unloadWorld(args[1])) {
                        sender.sendMessage("World unloaded")
                    } else {
                        sender.sendMessage("An error occurred while unloading world")
                    }
                }
                "clone" -> {
                    if (args.size != 3) {
                        sender.sendMessage("Invalid usage")
                        return false
                    }
                    sender.sendMessage("Cloning world")
                    val res = simpleWorldManagerApi.cloneWorld(args[1], args[2])
                    when (res) {
                        CloneWorldResponse.SUCCESS -> { sender.sendMessage("Cloning finished") }
                        CloneWorldResponse.COPY_ERROR -> { sender.sendMessage("Error occurred while copying world folder") }
                        CloneWorldResponse.NONEXISTENT_WORLD -> { sender.sendMessage("Source world does not exist") }
                        CloneWorldResponse.WORLD_EXISTS -> { sender.sendMessage("Destination world already exists") }
                        CloneWorldResponse.DIRECTORY_EXISTS -> { sender.sendMessage("Destination directory already exists") }
                    }
                }
                "remove" -> {
                    if (args.size == 2) {
                        sender.sendMessage("This will delete all data for the world \"${args[1]}\"")
                        sender.sendMessage("You will lose all access to this world and it's contents.")
                        val text = ComponentBuilder("To confirm run: ")
                        val clickText = TextComponent("/${command.label} remove ${args[1]} confirm")
                        clickText.clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/${command.label} remove ${args[1]} confirm")
                        text.append(clickText)
                        sender.spigot().sendMessage(*text.create())
                    } else if (args.size == 3) {
                        if (args[2] == "confirm") {
                            sender.sendMessage("Removing world")
                            simpleWorldManagerApi.removeWorld(args[1])
                            sender.sendMessage("World removed")
                        } else {
                            sender.sendMessage("Invalid usage")
                            return false
                        }
                    } else {
                        sender.sendMessage("Invalid usage")
                        return false
                    }
                }
                "linking" -> {
                    if (args.size != 2) {
                        sender.sendMessage("Invalid usage")
                        return false
                    }
                    when (args[1]) {
                        "enable" -> {
                            enablePortalLinking = true
                            sender.sendMessage("Portal linking enabled")
                        }
                        "disable" -> {
                            enablePortalLinking = false
                            sender.sendMessage("Portal linking disabled")
                        }
                        else -> {
                            sender.sendMessage("Invalid usage")
                            return false
                        }
                    }
                    sender.sendMessage("Please restart the server for the changes to apply")
                }
                "link" -> {
                    if (args.size != 4) {
                        sender.sendMessage("Invalid usage")
                        return false
                    }
                    sender.sendMessage("Linking worlds")
                    if (args[3] !in arrayOf("nether", "end")) {
                        sender.sendMessage("Nonexistent link type")
                    } else {
                        when (
                            simpleWorldManagerApi.linkWorlds(
                                args[1], args[2],
                                when (args[3]) {
                                    "nether" -> LinkPortalType.NETHER
                                    "end" -> LinkPortalType.END
                                    else -> LinkPortalType.NETHER
                                } )
                        ) {
                            LinkWorldsResponse.NONEXISTENT_SOURCE -> { sender.sendMessage("Source world does not exist") }
                            LinkWorldsResponse.NONEXISTENT_DESTINATION -> { sender.sendMessage("Destination world does not exist") }
                            LinkWorldsResponse.SUCCESS -> { sender.sendMessage("Worlds linked") }
                        }
                    }
                }
                "import" -> {
                    if (args.size != 2) {
                        sender.sendMessage("Invalid usage")
                        return false
                    }
                    sender.sendMessage("Importing world")
                    sender.sendMessage("NOTE: This is experimantal and may break stuff")

                    when(
                        simpleWorldManagerApi.importWorld(
                            args[1]
                        )
                    ) {
                        ImportWorldResponse.IMPORTING_ERROR -> { sender.sendMessage("An error occurred while importing") }
                        ImportWorldResponse.IS_FILE -> { sender.sendMessage("The specified world is a file") }
                        ImportWorldResponse.NONEXISTENT_FOLDER -> { sender.sendMessage("The specified world folder doesn't exist") }
                        ImportWorldResponse.ALREADY_IMPORTED -> { sender.sendMessage("The specified world is already imported") }
                        ImportWorldResponse.SUCCESS -> { sender.sendMessage("World imported") }
                    }

                }
                "forceload" -> {
                    if (args.size != 3) {
                        sender.sendMessage("Invalid usage")
                        return false
                    }

                    when (args[1]) {
                        "add" -> {
                            sender.sendMessage("Adding world to the forceload list")
                            if (!simpleWorldManagerApi.checkWorldExists(args[2])) {
                                sender.sendMessage("World with that name doesn't exist")
                            } else if (args[2] in forceLoad) {
                                sender.sendMessage("World already in the forceload list")
                            } else {
                                forceLoad.add(args[2])
                                sender.sendMessage("World added")
                            }
                        }
                        "remove", "rem" -> {
                            sender.sendMessage("Removing world from forceload list")
                            if (args[2] in forceLoad) {
                                forceLoad.remove(args[2])
                                sender.sendMessage("World removed")
                            } else {
                                sender.sendMessage("World not in the forceload list")
                            }

                        }
                    }
                }
                "spawn" -> {
                    when (args[1]) {
                        "override" -> {
                            if (args.size != 3) {
                                sender.sendMessage("Invalid usage")
                                return false
                            }
                            when (args[2]) {
                                "enable" -> {
                                    enableSpawnOverride = true
                                    sender.sendMessage("Spawn override enabled")
                                }
                                "disable" -> {
                                    enableSpawnOverride = false
                                    sender.sendMessage("Spawn override disabled")
                                }
                                else -> {
                                    sender.sendMessage("Invalid usage")
                                    return false
                                }
                            }
                            sender.sendMessage("Please restart the server for the changes to apply")
                        }
                        "reset" -> {
                            globalSpawn = Bukkit.getWorld("world")!!.spawnLocation
                            sender.sendMessage("Spawn reset")
                        }
                        "set" -> {
                            if (args.size != 4 && args.size != 6 && args.size != 8) {
                                sender.sendMessage("Invalid usage")
                                return false
                            }
                            if (args[3] == "default") {
                                when (simpleWorldManagerApi.setServerSpawn(args[2])) {
                                    SetServerSpawnResponse.UNLOADED_WORLD -> { sender.sendMessage("World is not loaded") }
                                    SetServerSpawnResponse.NONEXISTENT_WORLD -> { sender.sendMessage("World with that name doesn't exist") }
                                    SetServerSpawnResponse.SUCCESS -> { sender.sendMessage("Spawn was successfully set") }
                                }
                            } else if (args.size == 6) {
                                val x = args[3].toDoubleOrNull()
                                val y = args[4].toDoubleOrNull()
                                val z = args[5].toDoubleOrNull()

                                if (x == null) { sender.sendMessage("X must be a number"); return true }
                                if (y == null) { sender.sendMessage("Y must be a number"); return true }
                                if (z == null) { sender.sendMessage("Z must be a number"); return true }

                                when (simpleWorldManagerApi.setServerSpawn(args[2], x, y, z)) {
                                    SetServerSpawnResponse.UNLOADED_WORLD -> { sender.sendMessage("World is not loaded") }
                                    SetServerSpawnResponse.NONEXISTENT_WORLD -> { sender.sendMessage("World with that name doesn't exist") }
                                    SetServerSpawnResponse.SUCCESS -> { sender.sendMessage("Spawn was successfully set") }
                                }
                            } else {
                                val x = args[3].toDoubleOrNull()
                                val y = args[4].toDoubleOrNull()
                                val z = args[5].toDoubleOrNull()
                                val yaw = args[6].toFloatOrNull()
                                val pitch = args[7].toFloatOrNull()

                                if (x == null) { sender.sendMessage("X must be a number"); return true }
                                if (y == null) { sender.sendMessage("Y must be a number"); return true }
                                if (z == null) { sender.sendMessage("Z must be a number"); return true }
                                if (yaw == null) { sender.sendMessage("Yaw must be a number"); return true }
                                if (pitch == null) { sender.sendMessage("Pitch must be a number"); return true }

                                when (simpleWorldManagerApi.setServerSpawn(args[2], x, y, z, yaw, pitch)) {
                                    SetServerSpawnResponse.UNLOADED_WORLD -> { sender.sendMessage("World is not loaded") }
                                    SetServerSpawnResponse.NONEXISTENT_WORLD -> { sender.sendMessage("World with that name doesn't exist") }
                                    SetServerSpawnResponse.SUCCESS -> { sender.sendMessage("Spawn was successfully set") }
                                }
                            }
                        }
                    }
                }
                else -> {
                    return false
                }
            }
        } else {
            return false
        }
        return true
    }
}