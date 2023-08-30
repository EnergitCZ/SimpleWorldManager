package dev.energit.SimpleWorldManager

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import java.io.File

enum class CloneWorldResponse {
    SUCCESS,
    NONEXISTENT_WORLD,
    COPY_ERROR,
    WORLD_EXISTS,
    DIRECTORY_EXISTS
}

enum class LoadWorldResponse {
    SUCCESS,
    NONEXISTENT_WORLD,
    LOAD_ERROR
}

enum class CreateWorldResponse {
    SUCCESS,
    WORLD_EXISTS,
    FILE_EXISTS,
    CREATION_ERROR
}

enum class RemoveWorldResponse {
    SUCCESS,
    NONEXISTENT_WORLD,
    FILE_REMOVAL_ERROR
}

enum class TeleportToWorldResponse {
    SUCCESS,
    NONEXISTENT_WORLD,
    UNLOADED_WORLD
}

enum class LinkPortalType {
    NETHER,
    END
}

enum class LinkWorldsResponse {
    SUCCESS,
    NONEXISTENT_SOURCE,
    NONEXISTENT_DESTINATION
}

enum class ImportWorldResponse {
    SUCCESS,
    NONEXISTENT_FOLDER,
    IS_FILE,
    ALREADY_IMPORTED,
    IMPORTING_ERROR
}

enum class SetServerSpawnResponse {
    SUCCESS,
    NONEXISTENT_WORLD,
    UNLOADED_WORLD
}

private fun makeFilenameSafe(name: String) : String {
    return name.replace(Regex("\\W+"), "")
}

class SimpleWorldManagerApi {
    fun createWorld(name: String) : CreateWorldResponse {
        /*
        Create a new world with default settings
         */
        val sname = makeFilenameSafe(name)

        if (File(sname).exists()) {
            return CreateWorldResponse.FILE_EXISTS
        }

        if (sname in worldsConfig) {
            return CreateWorldResponse.WORLD_EXISTS
        }

        logger.info("Creating level \"$sname\"")

        val w = WorldCreator(sname)
            .createWorld()
            ?: return CreateWorldResponse.CREATION_ERROR

        worldsConfig[sname] = WorldConfig(
            name,
            w.environment,
            w.seed,
            "world_nether",
            "world_the_end"
        )

        worldNames.add(sname)

        logger.info("Creation of level \"$sname\" finished")
        return CreateWorldResponse.SUCCESS
    }

    fun createWorld(name: String, seed: Long) : CreateWorldResponse {
        /*
        Create a new world with a specific seed
         */
        val sname = makeFilenameSafe(name)

        if (File(sname).exists()) {
            return CreateWorldResponse.FILE_EXISTS
        }

        if (sname in worldsConfig) {
            return CreateWorldResponse.WORLD_EXISTS
        }

        logger.info("Creating level \"$sname\"")

        val w = WorldCreator(sname)
            .seed(seed)
            .createWorld()
            ?: return CreateWorldResponse.CREATION_ERROR

        worldsConfig[sname] = WorldConfig(
            name,
            w.environment,
            w.seed,
            "world_nether",
            "world_the_end"
        )

        worldNames.add(sname)

        logger.info("Creation of level \"$sname\" finished")
        return CreateWorldResponse.SUCCESS
    }

    fun createWorld(name: String, environment: World.Environment) : CreateWorldResponse {
        /*
        Create a new world with a specific environment
         */
        val sname = makeFilenameSafe(name)

        if (File(sname).exists()) {
            return CreateWorldResponse.FILE_EXISTS
        }

        if (sname in worldsConfig) {
            return CreateWorldResponse.WORLD_EXISTS
        }

        logger.info("Creating level \"$sname\"")

        val w = WorldCreator(sname)
            .environment(environment)
            .createWorld()
            ?: return CreateWorldResponse.CREATION_ERROR

        worldsConfig[sname] = WorldConfig(
            name,
            w.environment,
            w.seed,
            "world_nether",
            "world_the_end"
        )

        worldNames.add(sname)

        logger.info("Creation of level \"$sname\" finished")
        return CreateWorldResponse.SUCCESS
    }

    fun createWorld(name: String, seed: Long, environment: World.Environment) : CreateWorldResponse {
        /*
        Create a new world with a specific seed and environment
         */
        val sname = makeFilenameSafe(name)

        if (File(sname).exists()) {
            return CreateWorldResponse.FILE_EXISTS
        }

        if (sname in worldsConfig) {
            return CreateWorldResponse.WORLD_EXISTS
        }

        logger.info("Creating level \"$sname\"")

        val w = WorldCreator(sname)
            .seed(seed)
            .environment(environment)
            .createWorld()
            ?: return CreateWorldResponse.CREATION_ERROR

        worldsConfig[sname] = WorldConfig(
            name,
            w.environment,
            w.seed,
            "world_nether",
            "world_the_end"
        )

        worldNames.add(sname)

        logger.info("Creation of level \"$sname\" finished")
        return CreateWorldResponse.SUCCESS
    }

    fun loadWorld(name: String) : LoadWorldResponse {
        /*
        Load an unloaded world
         */
        val sname = makeFilenameSafe(name)
        logger.info("Loading level \"$sname\"")

        val wc = worldsConfig[sname] ?: return LoadWorldResponse.NONEXISTENT_WORLD

        WorldCreator(sname)
            .seed(wc.seed)
            .environment(wc.environment)
            .createWorld() ?: return LoadWorldResponse.LOAD_ERROR
        logger.info("Loading of level \"$sname\" finished")
        return LoadWorldResponse.SUCCESS
    }

    fun unloadWorld(name: String) : Boolean {
        /*
        Unload a loaded world
         */
        val sname = makeFilenameSafe(name)
        logger.info("Unloading level \"$sname\"")
        val ret = Bukkit.unloadWorld(sname, true)
        if (!ret) {
            return false
        }
        logger.info("Level unloaded")
        return true
    }

    fun teleportPlayerIntoWorld(player: Player, worldName: String) : TeleportToWorldResponse {
        /*
        Teleport a player to a worlds spawn
         */

        val sname = makeFilenameSafe(worldName)

        if (sname !in worldsConfig) {
            return TeleportToWorldResponse.NONEXISTENT_WORLD
        }

        val world = Bukkit.getWorld(sname) ?: return TeleportToWorldResponse.UNLOADED_WORLD
        player.teleport(world.spawnLocation)

        return TeleportToWorldResponse.SUCCESS
    }

    fun cloneWorld(worldNameSource: String, worldNameDest: String) : CloneWorldResponse {
        /*
        Make a copy of an existing world
         */
        val worldName = makeFilenameSafe(worldNameDest)
        val worldSource = makeFilenameSafe(worldNameSource)

        if (File(worldName).exists()) {
            return CloneWorldResponse.DIRECTORY_EXISTS
        }

        if (worldSource !in worldsConfig) {
            return CloneWorldResponse.NONEXISTENT_WORLD
        }
        if (worldName in worldsConfig) {
            return CloneWorldResponse.WORLD_EXISTS
        }

        logger.info("Cloning level \"$worldSource\" into \"$worldName\"")

        val world = Bukkit.getWorld(worldSource) ?: return CloneWorldResponse.NONEXISTENT_WORLD

        try {
            if (!world.worldFolder.copyRecursively(File(worldName))) {
                return CloneWorldResponse.COPY_ERROR
            }
        } catch (e: Exception) {
            if (e is FileAlreadyExistsException) {
                return CloneWorldResponse.DIRECTORY_EXISTS
            }
        }

        File("$worldName/uid.dat").delete()

        worldsConfig[worldName] = worldsConfig[worldSource]!!

        worldNames.add(worldName)

        loadWorld(worldName)

        logger.info("Cloning finished")

        return CloneWorldResponse.SUCCESS
    }

    fun removeWorld(name: String) : RemoveWorldResponse {
        /*
        Remove all data of a world
         */
        val sname = makeFilenameSafe(name)

        if (sname !in worldsConfig) {
            return RemoveWorldResponse.NONEXISTENT_WORLD
        }

        logger.info("Removing level \"$sname\"")

        Bukkit.unloadWorld(sname, true)

        if (!File(sname).delete()) {
            return RemoveWorldResponse.FILE_REMOVAL_ERROR
        }

        logger.info("Level removed")

        return RemoveWorldResponse.SUCCESS
    }

    fun linkWorlds(sourceWorldName: String, destinationWorldName: String, portalType: LinkPortalType) : LinkWorldsResponse {
        /*
        Link nether and end portals of worlds
         */
        val sourceWorld = makeFilenameSafe(sourceWorldName)
        val destinationWorld = makeFilenameSafe(destinationWorldName)

        if (sourceWorld !in worldsConfig) {
            return LinkWorldsResponse.NONEXISTENT_SOURCE
        }
        if (destinationWorld !in worldsConfig) {
            return LinkWorldsResponse.NONEXISTENT_DESTINATION
        }

        when (portalType) {
            LinkPortalType.NETHER -> { worldsConfig[sourceWorld]!!.portalNether = destinationWorld }
            LinkPortalType.END -> { worldsConfig[sourceWorld]!!.portalEnd = destinationWorld }
        }

        return LinkWorldsResponse.SUCCESS
    }

    fun importWorld(name: String) : ImportWorldResponse {
        /*
        Import a world from a folder
         */
        val sname = makeFilenameSafe(name)

        logger.info("Importing level \"$sname\"")
        logger.info("NOTE: Importing can break things")

        if (sname in worldsConfig) {
            return ImportWorldResponse.ALREADY_IMPORTED
        }

        if (File(sname).exists()) {
            if (File(sname).isFile) {
                return ImportWorldResponse.IS_FILE
            }
        } else {
            return ImportWorldResponse.NONEXISTENT_FOLDER
        }

        File("$sname/uid.dat").delete()

        val w = WorldCreator(sname)
            .createWorld()
            ?: return ImportWorldResponse.IMPORTING_ERROR

        worldsConfig[sname] = WorldConfig(
            name,
            w.environment,
            w.seed,
            "world_nether",
            "world_the_end"
        )

        worldNames.add(sname)

        logger.info("Importing of level \"$sname\" finished")
        return ImportWorldResponse.SUCCESS
    }

    fun checkWorldExists(name: String) : Boolean {
        /*
        Check if a world exists
         */
        val sname = makeFilenameSafe(name)
        return sname in worldsConfig
    }

    fun checkWorldLoaded(name: String) : Boolean {
        /*
        Check if a world is loaded
         */
        val sname = makeFilenameSafe(name)
        Bukkit.getWorld(sname) ?: return false
        return true
    }

    fun setServerSpawn(worldName: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) : SetServerSpawnResponse {
        /*
        Set global spawn of a server
         */

        val sname = makeFilenameSafe(worldName)
        if (!checkWorldExists(sname)) {
            return SetServerSpawnResponse.NONEXISTENT_WORLD
        }

        val world = Bukkit.getWorld(sname) ?: return SetServerSpawnResponse.UNLOADED_WORLD

        globalSpawn = Location(world, x, y, z, yaw, pitch)

        return SetServerSpawnResponse.SUCCESS
    }

    fun setServerSpawn(worldName: String, x: Double, y: Double, z: Double) : SetServerSpawnResponse {
        /*
        Set global spawn of a server
         */

        val sname = makeFilenameSafe(worldName)
        if (!checkWorldExists(sname)) {
            return SetServerSpawnResponse.NONEXISTENT_WORLD
        }

        val world = Bukkit.getWorld(sname) ?: return SetServerSpawnResponse.UNLOADED_WORLD

        globalSpawn = Location(world, x, y, z)

        return SetServerSpawnResponse.SUCCESS
    }

    fun setServerSpawn(worldName: String) : SetServerSpawnResponse {
        /*
        Set global spawn of a server
         */

        val sname = makeFilenameSafe(worldName)
        if (!checkWorldExists(sname)) {
            return SetServerSpawnResponse.NONEXISTENT_WORLD
        }

        val world = Bukkit.getWorld(sname) ?: return SetServerSpawnResponse.UNLOADED_WORLD

        globalSpawn = world.spawnLocation

        return SetServerSpawnResponse.SUCCESS
    }
}

val simpleWorldManagerApi = SimpleWorldManagerApi()