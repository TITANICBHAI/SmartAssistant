package com.aiassistant.models;

/**
 * Types of games supported by the AI assistant
 */
public enum GameType {
    
    NONE("None"),
    FPS("First-Person Shooter"),
    RPG("Role-Playing Game"),
    MOBA("Multiplayer Online Battle Arena"),
    STRATEGY("Strategy"),
    RACING("Racing"),
    SPORTS("Sports"),
    CASUAL("Casual"),
    PUZZLE("Puzzle"),
    ARCADE("Arcade"),
    CARD("Card Game"),
    ADVENTURE("Adventure"),
    SIMULATION("Simulation"),
    PUBG_MOBILE("PUBG Mobile"),
    CALL_OF_DUTY_MOBILE("Call of Duty Mobile"),
    FREE_FIRE("Free Fire"),
    MOBILE_LEGENDS("Mobile Legends"),
    POKEMON_UNITE("Pokemon Unite"),
    AMONG_US("Among Us"),
    GENSHIN_IMPACT("Genshin Impact"),
    ROBLOX("Roblox"),
    OTHER("Other Game");
    
    private final String displayName;
    
    /**
     * Constructor
     * 
     * @param displayName Display name for the game type
     */
    GameType(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Get display name
     * 
     * @return Display name for the game type
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get game type from package name
     * 
     * @param packageName Package name
     * @return Game type or NONE if not a game
     */
    public static GameType fromPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return NONE;
        }
        
        packageName = packageName.toLowerCase();
        
        if (packageName.contains("pubg")) {
            return PUBG_MOBILE;
        } else if (packageName.contains("callofduty") || packageName.contains("cod")) {
            return CALL_OF_DUTY_MOBILE;
        } else if (packageName.contains("freefire") || packageName.contains("garena")) {
            return FREE_FIRE;
        } else if (packageName.contains("mobilelegends") || packageName.contains("moonton")) {
            return MOBILE_LEGENDS;
        } else if (packageName.contains("pokemon") && packageName.contains("unite")) {
            return POKEMON_UNITE;
        } else if (packageName.contains("genshin") || packageName.contains("mihoyo")) {
            return GENSHIN_IMPACT;
        } else if (packageName.contains("amongus") || packageName.contains("innersloth")) {
            return AMONG_US;
        } else if (packageName.contains("roblox")) {
            return ROBLOX;
        } else if (packageName.contains("fps") || packageName.contains("shooter")) {
            return FPS;
        } else if (packageName.contains("rpg") || packageName.contains("role")) {
            return RPG;
        } else if (packageName.contains("moba") || packageName.contains("battle") || packageName.contains("arena")) {
            return MOBA;
        } else if (packageName.contains("strategy") || packageName.contains("tower") || packageName.contains("defense")) {
            return STRATEGY;
        } else if (packageName.contains("racing") || packageName.contains("speed") || packageName.contains("asphalt")) {
            return RACING;
        } else if (packageName.contains("sports") || packageName.contains("football") || packageName.contains("soccer") || 
                  packageName.contains("basketball") || packageName.contains("fifa")) {
            return SPORTS;
        } else if (packageName.contains("casual") || packageName.contains("idle")) {
            return CASUAL;
        } else if (packageName.contains("puzzle") || packageName.contains("brain") || packageName.contains("match")) {
            return PUZZLE;
        } else if (packageName.contains("arcade") || packageName.contains("retro")) {
            return ARCADE;
        } else if (packageName.contains("card") || packageName.contains("poker") || packageName.contains("solitaire")) {
            return CARD;
        } else if (packageName.contains("adventure") || packageName.contains("quest")) {
            return ADVENTURE;
        } else if (packageName.contains("simulation") || packageName.contains("simulator") || packageName.contains("tycoon")) {
            return SIMULATION;
        } else if (isGamePackageName(packageName)) {
            return OTHER;
        }
        
        return NONE;
    }
    
    /**
     * Check if a package name is a game
     * 
     * @param packageName Package name
     * @return true if it's likely a game
     */
    public static boolean isGamePackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        packageName = packageName.toLowerCase();
        
        // Keywords often found in game package names
        String[] gameKeywords = {
            "game", "play", "studio", "entertainment", "interactive", "fps", "rpg", "mmorpg", 
            "shooter", "arcade", "puzzle", "race", "racing", "sport", "casino", "card", 
            "battle", "quest", "saga", "crush", "craft", "royale", "clash", "legend", 
            "warrior", "zombie", "hero", "run", "adventure", "simulator", "tower", "defense", 
            "idle", "clicker", "tap", "strategy", "war", "tank", "ninja", "bird", "farm"
        };
        
        // Major game publishers
        String[] gamePublishers = {
            "gameloft", "zynga", "ea.", "electronicarts", "ubisoft", "activision", "tencent", 
            "supercell", "rovio", "king", "niantic", "com.playrix", "habby", "voodoo", 
            "moonton", "nexon", "netmarble", "bandai", "nplus", "outfit7", "miniclip", 
            "halfbrick", "ketchapp", "devsisters", "glu", "nintendo", "square", "enix", 
            "epic", "gamevil", "netease", "com.mojang", "sega", "capcom", "com.rockstar"
        };
        
        // Check for game keywords
        for (String keyword : gameKeywords) {
            if (packageName.contains(keyword)) {
                return true;
            }
        }
        
        // Check for game publishers
        for (String publisher : gamePublishers) {
            if (packageName.contains(publisher)) {
                return true;
            }
        }
        
        return false;
    }
}