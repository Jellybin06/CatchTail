package org.kw906plugin.catchTail.commands;

import net.kyori.adventure.text.Component;
//import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import org.kw906plugin.catchTail.CatchTail;
import org.kw906plugin.catchTail.GameStatus;
import org.kw906plugin.catchTail.PlayerData;
import org.kw906plugin.catchTail.SendMessage;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getOnlinePlayers;
import static org.kw906plugin.catchTail.utils.PlayerImpl.getPlayerByName;
import static org.kw906plugin.catchTail.utils.UpdateWorldBorder.setWorldBorder;

public class Sequence {
    private static PlayerData playerData = new PlayerData();
    private static int gameTime = 0;
    private static int countDown = -1;

    public static void init()
    {
        if (GameStatus.getStatus().equals(GameStatus.INITIALIZED)) {
            SendMessage.sendMessageOP(Component.text("이미 게임이 초기화 되어있습니다!")
                                               .color(NamedTextColor.RED));
            return;
        }

        SendMessage.broadcastMessage(Component.text("시퀀스 - 게임 초기화 진행")
                                              .color(NamedTextColor.GRAY)
        );

        try {
            SendMessage.broadcastMessage(Component.text("시퀀스 - 오버월드 월드보더 설정 중..")
                                                  .color(NamedTextColor.GRAY));
            setWorldBorder(CatchTail.config.getOverworld(), CatchTail.config.overworldWorldBorderSize);

            SendMessage.broadcastMessage(Component.text("시퀀스 - 네더 월드보더 설정 중..")
                                                  .color(NamedTextColor.GRAY));
            setWorldBorder(CatchTail.config.getNether(), CatchTail.config.netherWorldBorderSize);

            SendMessage.broadcastMessage(Component.text("시퀀스 - 엔드 월드보더 설정 중..")
                                                  .color(NamedTextColor.GRAY));
            setWorldBorder(CatchTail.config.getTheEnd(), CatchTail.config.endWorldBorderSize);

            SendMessage.broadcastMessage(Component.text("시퀀스 - 플레이어 추가 중..")
                                                  .color(NamedTextColor.GRAY));
            for (Player player: getOnlinePlayers()) {
                playerData.add(player);
                SendMessage.broadcastMessage(Component.text(
                        "시퀀스 - " + player.getName() + "을 대기 목록에 추가하였습니다."));
            }
        } catch (NullPointerException e) {
            SendMessage.broadcastMessage(Component.text("게임 초기화 도중 오류가 발생하였습니다.")
                                                  .color(NamedTextColor.RED));
            SendMessage.broadcastMessage(Component.text("오류 내용: " + e.getMessage())
                                                  .color(NamedTextColor.RED));
        }

        GameStatus.setStatus(GameStatus.INITIALIZED);
        SendMessage.broadcastMessage(Component.text("시퀀스 - 게임 초기화가 완료되었습니다.")
                                              .color(NamedTextColor.GRAY));
    }

    public static void start() {
        SendMessage.broadcastMessage(Component.text("시퀀스 - 게임이 시작됩니다!").color(NamedTextColor.BLUE));
        cleanup();
        setup();
        startGame();
    }

    public static void stop() {
        SendMessage.broadcastMessage(Component.text("시퀀스 - 게임이 중지됩니다!")
                                              .color(NamedTextColor.RED));
        GameStatus.setStatus(GameStatus.STOPPED);
        cleanup();
    }

    public static void unregister(String[] args)
    {
        for (String playerName : args) {
            Player playerToRemove = getPlayerByName(playerName);
            if (playerToRemove != null) {
                playerData.removePlayer(playerToRemove);
                SendMessage.broadcastMessage(Component.text(playerName + " - 플레이어가 대기 목록에서 제외되었습니다.")
                                                      .color(NamedTextColor.RED));
            } else {
                SendMessage.broadcastMessage(Component.text(playerName + " - 플레이어를 찾을 수 없습니다.")
                                                      .color(NamedTextColor.RED));
            }
        }
    }

    public static void config(String[] args) {
        if (args.length == 0) {
            SendMessage.sendMessageOP(Component.text("설정 목록:"));
            for (Field field : CatchTail.config.getClass().getFields()) {
                try {
                    Object value = field.get(CatchTail.config);

                    SendMessage.sendMessageOP(
                            Component.text(field.getName() + ": ")
                                     .color(NamedTextColor.AQUA)
                                     .append(Component.text(value.toString())
                                                      .color(NamedTextColor.GRAY)));
                } catch (IllegalAccessException e) {
                    getLogger().log(Level.WARNING, e.getMessage());
                }
            }
        }

        Optional<Field> configOptional = Arrays.stream(CatchTail.config.getClass().getFields())
                                               .filter(f -> f.getName().equals(args[0])).findAny();
        if (configOptional.isPresent()) {
            try {
                Field targetConfig = configOptional.get();
                String configType = targetConfig.getType().getSimpleName();
                String beforeValue = targetConfig.get(CatchTail.config).toString();
                boolean success = false;
                switch (configType) {
                    case "String":
                        targetConfig.set(CatchTail.config, args[1]);
                        success = true;
                        break;
                    case "int":
                        try {
                            targetConfig.set(CatchTail.config, Integer.parseInt(args[1]));
                            success = true;
                        } catch (NumberFormatException e) {
                            SendMessage.sendMessageOP(Component.text("잘못된 입력 값으로 인해 변경에 실패하였습니다.\n" +
                                                                             "요청된 값: " + args[1])
                                                               .color(NamedTextColor.RED));
                        }
                        break;
                    default:
                        SendMessage.sendMessageOP(Component.text("구현되지 않은 요청입니다. (요청된 타입: " + configType + ")")
                                                           .color(NamedTextColor.DARK_GRAY));
                        break;
                }

                if (success) {
                    SendMessage.sendMessageOP(Component.text("설정이 성공적으로 변경되었습니다!"));
                    SendMessage.sendMessageOP(Component.text("변경 내용: (" + beforeValue + "→" + args[1] + ")"));
                }
            } catch (IllegalAccessException e) {
                SendMessage.sendMessageOP(Component.text("설정을 변경하던 중 오류가 발생하였습니다.\n" +
                                                                 "자세한 내용은 서버 로그를 참고하세요.")
                                                   .color(NamedTextColor.RED));
                getLogger().log(Level.WARNING, e.getMessage());
            }
        }
    }

    public static void setup()
    {
        SendMessage.sendMessageOP(Component.text("시퀀스 - 셋업 진행중")
                                           .color(NamedTextColor.GRAY)
        );
        GameStatus.setStatus(GameStatus.ENGINE_SETUP);
//
//        GameBorder.setborder();
//
//        if (!ItemDistribute.DistributeItems())
//            return;
//        gameTime = 0;
//
//        // Broadcaset Setting
//        if (GameSettings.BROADCAST_CONFIG.value() == 1)
//            GameSettings.broadcastSettingToOp();
//
        // Remove Dropped Item
        for (Entity current : CatchTail.config.getOverworld().getEntities()) {
            if (current instanceof Item) {
                current.remove();
            }
            if (current instanceof Creature) {
                current.remove();
            }
        }
//        for (Player player : Bukkit.getOnlinePlayers()) {
//            if (player.getGameMode() != GameMode.SURVIVAL)
//                continue;
//
//            // Distribute Item
//            player.getInventory().clear();
//            player.getInventory().addItem(SpecialItems.WOOD_SWORD.get());
//
//            // SETUP REMAING TIME
//            Logger logger = Bukkit.getServer().getLogger();
//            logger.info(">> Setting up user : " + player.getName());
//
//            int lifeTime = (int) Math
//                    .floor(Math.random() * (GameSettings.MAX_LIFE.seconds() - GameSettings.MIN_LIFE.seconds() + 1))
//                    + GameSettings.MIN_LIFE.seconds();
//            // playerRemaingTime.put(player, lifeTime);
//            updateTime(player, lifeTime);
//
//            // FINAL STEP : Add to Playing List
//            playerPlaying.add(player);
//        }
//
//        if (GameSettings.ADD_NULL_PLAYER.value() == 1) {
//            playerRemaingTime.put(null, 60);
//            playerPlaying.add(null);
//        }
//
//        SendMessage.broadcastMessage(ChatColor.GREEN + "게임 준비 완료 -- 시작!");
//        GameSettings.gameInfo();
//
//        countDown = (int) GameSettings.COUNTDOWN_TIME.value();
//        GameStatus.setStatus(GameStatus.COUNT_DOWN);
    }

    public static void startGame() {

        SendMessage.broadcastMessage(Component.text("10초 후 랜덤한 위치로 텔레포트합니다.")
                .color(NamedTextColor.BLUE));

        for (int i = 9; i > 0; i--) {
            int countdown = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    SendMessage.broadcastMessage(Component.text(countdown + "초")
                                    .color(NamedTextColor.AQUA));
                }
            }.runTaskLater(JavaPlugin.getProvidingPlugin(CatchTail.class), (10 - i) * 20L);  // 1초(20 틱) 간격으로 실행
        }

        // 10초 후에 모든 플레이어를 랜덤한 위치로 텔레포트
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorlds().get(0);
                List<Location> locations = new ArrayList<>();
                Random random = new Random();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    Location randomLocation = getRandomLocation(world, random, locations);
                    locations.add(randomLocation);
                    player.teleport(randomLocation);
                }

                SendMessage.broadcastMessage(Component.text("모든 인원이 텔레포트되었습니다.")
                        .color(NamedTextColor.BLUE));
            }
        }.runTaskLater(JavaPlugin.getProvidingPlugin(CatchTail.class), 190L);  // 10초 후 실행 (200 틱)
    }


    private static Location getRandomLocation(World world, Random random, List<Location> locations) {
        Location location;
        boolean isSafe;

        do {
            double x = random.nextInt(50000) - 25000;
            double z = random.nextInt(50000) - 25000;
            location = new Location(world, x, 0, z);
            location.setY(world.getHighestBlockAt(location).getY() + 1);

            isSafe = isSafeLocation(location, locations);

        } while (!isSafe);

        return location;
    }

    private static boolean isSafeLocation(Location location, List<Location> locations) {
        for (Location loc : locations) {
            if (loc.distance(location) < 2000) {
                return false;
            }
        }
        return true;
    }

    public static void cleanup() {
        playerData.cleanup();

        countDown = -1;
    }

    public static void out()
    {
        // 죽인 팀에게 흡수되도록 코드 작성..
    }
}
