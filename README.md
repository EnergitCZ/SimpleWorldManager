# SimpleWorldManager
It does what it says, manages worlds.

## It can:
 - Create worlds
 - Remove worlds
 - Teleport players between worlds
 - Load and unload worlds
 - Load worlds on startup
 - Clone worlds
 - Link nether and end portals
 - Import worlds
 - Set server spawn

## Installation
This plugin requires [SpiKot](https://github.com/EnergitCZ/SpiKot)
![Requires SpiKot](img/requires-spikot.svg)

## Usage

### Creating worlds
`/swm create <world name>`

`/swm create <world name> <seed>`

`/swm create <world name> <generator>`

`/swm create <world name> <seed> <generator>`

### Loading and unloading world
`/swm load <world name>`

`/swm unload <world name>`

### Load on startup
`/swm forceload add <world name>`

`/swm forceload remove <world name>`

`/swm forceload rem <world name>`

### Clone worlds
`/swm clone <source world name> <destination world name>`

### Remove world
`/swm remove <world name>`

### Teleport between worlds
`/swm tp <world name>`

`/swm tp player <world name>`

or

`/swm teleport <world name>`

`/swm teleport player <world name>`

### Enable or disable world linking
Enabled by default

`/swm linking enable`

`/swm linking disable`

### Link worlds
NOTE: Experimental

`/swm link <source world> <destination world> nether`

`/swm link <source world> <destination world> end`

### Import worlds
WARNING: Experimental

`/swm import <world folder>`

### Set server spawn

`/swm spawn override enable`

`/swm spawn override disable`

`/swm spawn reset`

`/swm spawn set <world name> <x> <y> <z>`

`/swm spawn set <world name> <x> <y> <z> <yaw> <pitch>`

`/swm spawn set <world name> default`

## API usage

### Installation

#### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.EnergitCZ</groupId>
    <artifactId>SimpleWorldManager</artifactId>
    <version>1.1.0</version>
    <scope>provided</scope>
</dependency>
```

#### Gradle 

##### Groovy
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.EnergitCZ:SimpleWorldManager:1.1.0'
}
```

##### Kotlin
```kotlin
repositories {
    maven("https://jitpack.io/")
}

dependencies {
    compileOnly("com.github.EnergitCZ:SimpleWorldManager:1.1.0")
}
```

### Usage

#### Get an instance of `SimpleWorldManagerApi`

##### Java
```java
SimpleWorldManagerApi getApi() {
    SimpleWorldManager swm = (SimpleWorldManager) Bukkit.getPluginManager().getPlugin("SimpleWorldManager");
    assert swm != null;
    return swm.getSimpleWorldManagerApi();
}
```

##### Kotlin
```kotlin
fun getApi() : SimpleWorldManagerApi? {
    val swm = Bukkit.getPluginManager().getPlugin("SimpleWorldManager") as SimpleWorldManager?
        ?: // Null check
        return null
    return swm.getSimpleWorldManagerApi()
}
```

#### Use it

For the time being just look into `src/main/kotlin/dev/energit/SimpleWorldManager/SimpleWorldManagerApi.kt` for info.

Or just use the recommendations from your IDE.
