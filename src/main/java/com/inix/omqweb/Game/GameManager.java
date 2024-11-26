package com.inix.omqweb.Game;

import com.inix.omqweb.Achievement.AchievementService;
import com.inix.omqweb.Beatmap.*;
import com.inix.omqweb.Beatmap.Alias.Alias;
import com.inix.omqweb.Beatmap.Alias.AliasType;
import com.inix.omqweb.Donation.Donation;
import com.inix.omqweb.Donation.DonationRepository;
import com.inix.omqweb.Donation.PlayerDonationDTO;
import com.inix.omqweb.History.*;
import com.inix.omqweb.Message.MessageService;
import com.inix.omqweb.Message.SystemMessageHandler;
import com.inix.omqweb.Util.AESUtil;
import com.inix.omqweb.Util.AnswerUtil;
import com.inix.omqweb.DTO.*;
import com.inix.omqweb.Util.ProfileUtil;
import com.inix.omqweb.osuAPI.Player;
import com.inix.omqweb.osuAPI.PlayerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Year;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameManager {
    private final BeatmapService beatmapService;
    private final MessageService messageService;
    private final PlayerRepository playerRepository;
    private final DonationRepository donationRepository;
    private final LobbyHistoryRepository lobbyHistoryRepository;
    private final LobbyHistoryDetailRepository lobbyHistoryDetailRepository;
    private final ResourceService resourceService;
    private final SystemMessageHandler systemMessageHandler;
    private final AESUtil aesUtil;

    private final Logger logger = LoggerFactory.getLogger(GameManager.class);
    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AchievementService achievementService;

    private final int MIN_COOLDOWN_TIME = 3;
    private final int MAX_COOLDOWN_TIME = 15;
    private final int MIN_GUESSING_TIME = 10;
    private final int MAX_GUESSING_TIME = 30;
    private final int MIN_TOTAL_QUESTIONS = 10;
    private final int MAX_TOTAL_QUESTIONS = 100;

    private boolean DEBUG_STATUS = false;

    private final ProfileUtil profileUtil;

    @PostConstruct
    private void createDebugLobbies() {
        if (!profileUtil.isDevEnv()) return;
        // Create lobbies for debug purpose
        for (int i = 0; i < 5; i++) {
            List<Player> players = playerRepository.findRandomPlayers();

            CreateGameDTO createGameDTO = CreateGameDTO.builder()
                    .name("Lobby " + (i + 1))
                    .totalQuestions(10)
                    .guessingTime(10)
                    .cooldownTime(5)
//                    .password("asd")
                    .displayMode(List.of(DisplayMode.AUDIO))
                    .poolMode(PoolMode.TOUHOU)
                    .difficulty(List.of(GameDifficulty.EASY, GameDifficulty.NORMAL, GameDifficulty.HARD, GameDifficulty.INSANE))
                    .startYear(2007)
                    .endYear(2021)
                    .build();

            createGame(createGameDTO, players.get(i));

            for (Player player : players) {
                joinGame(JoinGameDTO.builder().gameId(games.keySet().toArray()[i].toString()).build(), player);
            }
        }

        DEBUG_STATUS = true;
    }

    @PostConstruct
    @Scheduled(fixedRate = 1000 * 60)
    public void checkInactiveGames() {
        if (profileUtil.isDevEnv()) return;

        for (Game game : games.values()) {
            try {
                if (game.getLastActivity() == null) {
                    continue;
                }

                if (game.getPlayers().isEmpty()) {
                    deleteGame(game);
                    continue;
                }

                long diff = new Date().getTime() - game.getLastActivity().getTime();

                if (diff > 300000) {
                    logger.info("Game: " + game + " has been inactive for 5 minutes. Deleting game.");
                    deleteGame(game);
                    continue;
                }

                // Check inactive players
                for (Player player : game.getPlayers()) {
                    if (player.getLast_activity() == null) {
                        continue;
                    }

                    long playerDiff = new Date().getTime() - player.getLast_activity().getTime();

                    if (playerDiff > 1000 * 60 * 10) {
                        kickInactivePlayer(game, player);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to check for inactive games: ", e);
            }
        }
    }

    public Game createGame(CreateGameDTO createGameDTO, Player owner) {
        Game game = new Game();
        game.setUuid(UUID.randomUUID());

        setGameProperties(game, createGameDTO);

        if(createGameDTO.getPassword() != null && !createGameDTO.getPassword().isEmpty()) {
            game.setPrivate(true);
            game.setPassword(createGameDTO.getPassword());
        }

        game.setOwner(owner);
        game.setLastActivity(new Date());

        game.setGameMode(createGameDTO.getGameMode());
        game.setCreationDate(new Date());

        if(games.containsKey(game.getUuid())) {
            throw new IllegalArgumentException("Game already exists with id: " + game.getUuid().toString());
        }

        games.put(game.getUuid(), game);

        logger.info("Game created with id: " + game.getUuid().toString() + " by user: " + game.getOwner().getUsername());

        return game;
    }

    public int joinGame(JoinGameDTO joinGameDTO, Player player) {
        UUID gameId = UUID.fromString(joinGameDTO.getGameId());

        // Check if the game exists
        Game game = getGameById(gameId);
        if (game == null) {
            return Game.GAME_JOIN_STATUS_NOT_EXIST;
        }

        // Check if the user is in another game
        for (Game g : games.values()) {
            if (g.getPlayers().contains(player)) {
                if (joinGameDTO.isForceLeave()) {
                    // If the user is already in another game and forceLeave is true, remove the user from all games
                    leaveAllGames(game, player);
                    break;
                } else {
                    // If the user is already in another game and forceLeave is false, return
                    return Game.GAME_JOIN_STATUS_USER_ALREADY_IN_ANOTHER_GAME;
                }
            }
        }

        // Check if the game is private and the password is incorrect
        // Check if the password is null or empty
        if (game.isPrivate() &&
                (joinGameDTO.getPassword() == null
                        || joinGameDTO.getPassword().isEmpty()
                        || !joinGameDTO.getPassword().equals(game.getPassword()))
        ) {
            return Game.GAME_JOIN_STATUS_INCORRECT_PASSWORD;
        }

        // Check if the player is banned
        if (game.getBannedPlayers().contains(player)) {
            return Game.GAME_JOIN_STATUS_USER_IS_BANNED;
        }

        // Check if the player is already in the game
        if (game.getPlayers().contains(player)) {
            return Game.GAME_JOIN_STATUS_USER_ALREADY_EXISTS;
        }

        // If none of the above conditions are met, add the player to the game
        player.setLast_activity(new Date());

        game.getPlayers().add(player);

        game.getLeaderboard().putIfAbsent(player, new TotalGuessDTO()); // Initialize the player's score to 0

        updateActivity(game);

        messageService.sendSystemMessage(game.getUuid(), player.getUsername() + " has joined the game.");

        systemMessageHandler.onPlayersUpdate(game);

        logger.info("User: " + player.getUsername() + " joined game: " + gameId.toString());

        return Game.GAME_JOIN_STATUS_SUCCESS; // if the player successfully joined the game
    }

    public boolean joinGameLegacy(JoinGameLegacyDTO joinGameLegacyDTO) {
        UUID gameId = UUID.fromString(joinGameLegacyDTO.getGameId());
        Player player = playerRepository.findById(joinGameLegacyDTO.getUserId()).orElseThrow();

        Game game = getGameById(gameId);
        if (game == null) {
            return false;
        }

        if(game.getPlayers().contains(player)) {
//            return false;
        }

        game.getPlayers().add(player);

        messageService.sendSystemMessage(game.getUuid(), player.getUsername() + " has joined the game.");

        systemMessageHandler.onPlayersUpdate(game);

        logger.info("User: " + player.getUsername() + " joined game: " + gameId.toString());

        return true;
    }

    public boolean leaveGame(Game game, Player player) {
        // Check if the player exists in the game, remove if they do
        if (game.getPlayers().contains(player)) {
            game.getPlayers().remove(player);
        } else {
            // If the player does not exist in the game, return false
            return false;
        }

        if (game.getPlayers().isEmpty()) {
            // If the game is empty, delete the game
            logger.info("Game is empty. Deleting game with id: " + game.getUuid());
            deleteGame(game);
            return true;
        }

        // Check if the player was the owner of the game
        if (game.getOwner().getId().equals(player.getId())) {
            // If the player was the owner, transfer ownership to the next player
            Player newOwner = game.getPlayers().get(0);
            transferOwnership(game, newOwner);
        }

        updateActivity(game);

        messageService.sendSystemMessage(game.getUuid(), player.getUsername() + " has left the game.");

        systemMessageHandler.onPlayersUpdate(game);

        logger.info("User: " + player.getUsername() + " left game: " + game.getUuid());

        return true;
    }

    public boolean deleteGame(Game game) {
        if (game == null) {
            return false;
        }

        logger.info("Deleting game: " + game.getUuid().toString());
        systemMessageHandler.onGameStatusUpdate(game, "gameDeleted");

        // Cancel the scheduled future if it exists
        cancelSchedulers(game);

        games.remove(game.getUuid());

        return true;
    }

    public boolean submitAnswer(UUID gameId, String userId, String message) {
        if(userId == null || userId.isEmpty() || message == null || message.isEmpty()) {
            return false;
        }

        Game game = getGameById(gameId);
        if (game == null) {
            return false;
        }

        updateActivity(game);

        game.getPlayers().forEach(player -> {
            if(player.getId().equals(userId)) {
                player.setLast_activity(new Date());
            }
        });

        Player player = playerRepository.findById(userId).orElseThrow();

        synchronized (game) {
            if (!game.isPlaying() || !game.isGuessing()) {
                return false;
            }

            // Store the player's answer
            Long submittedTime = new Date().getTime();
            PlayerAnswer playerAnswer = PlayerAnswer.builder()
                    .answer(message)
                    .submittedTime(submittedTime)
                    .isCorrect(false)
                    .build();
            game.getPlayerAnswers().put(player, playerAnswer);

            ConcurrentHashMap<String, Boolean> answerSubmission = new ConcurrentHashMap<>();

            for (Map.Entry<Player, PlayerAnswer> entry : game.getPlayerAnswers().entrySet()) {
                answerSubmission.put(entry.getKey().getId(), entry.getValue().getSubmittedTime() != null);
            }

            systemMessageHandler.onAnswerSubmissionUpdate(game, answerSubmission);

            // Check if all players have answered
            if (game.getPlayerAnswers().size() == game.getPlayers().size() && !game.isAllAnswered() && game.isAutoskip()) {
                long delay = game.getGameScheduler_Guess().getDelay(TimeUnit.MILLISECONDS);
                if (delay < 3000) {
                    // If the delay (=time left) is less than 3 seconds, ignore
                    // This is to prevent multiple continueGame calls, aka autoskip bug
                    return true;
                }
                game.setAllAnswered(true);
                game.getGameScheduler_Guess().cancel(false);
                continueGame(game);
            }

        }

        return true;
    }

    public int checkAnswer(Game game) {
        int correctAnswerCount = 0;

        Beatmap beatmap = game.getBeatmaps().get(game.getQuestionIndex());

        for (Map.Entry<Player, PlayerAnswer> entry : game.getPlayerAnswers().entrySet()) {
            List<Alias> aliases = beatmap.getAliases();
            String answerToCheck;

            switch (game.getGameMode()) {
                case ARTIST -> {
                    answerToCheck = beatmap.getArtist();
                    aliases = aliases.stream()
                            .filter(alias -> alias.getAliasType() == AliasType.ARTIST)
                            .collect(Collectors.toList());
                }
                case CREATOR -> {
                    answerToCheck = beatmap.getCreator();
                    aliases = aliases.stream()
                            .filter(alias -> alias.getAliasType() == AliasType.CREATOR)
                            .collect(Collectors.toList());
                }
                default -> {
                    answerToCheck = beatmap.getTitle();
                    aliases = aliases.stream()
                            .filter(alias -> alias.getAliasType() == AliasType.TITLE)
                            .collect(Collectors.toList());
                }
            }

            boolean checkAnswer = false;
            boolean checkAliasAnswer = false;

            for (Alias alias : aliases) {
                String aliasName = alias.getName();
                double similarity = AnswerUtil.checkAnswer(entry.getValue().getAnswer(), aliasName);

                if (similarity >= 0.96d) {  // Using similarity over 0.96, might replace with exact match later?
                    checkAliasAnswer = true;
                    checkAnswer = true;
                    break;
                }
            }

            double similarity = AnswerUtil.checkAnswer(entry.getValue().getAnswer(), answerToCheck);

            if (similarity >= 0.96d) {
                checkAnswer = true;
                checkAliasAnswer = false;
            }

            if (checkAnswer) {
                correctAnswerCount++;
                entry.getValue().setCorrect(true);
                entry.getValue().setAliasCorrect(checkAliasAnswer);
            }
        }

        return correctAnswerCount;
    }

    private void continueGame(Game game) {
        Beatmap beatmap = game.getBeatmaps().get(game.getQuestionIndex());

        // Prevent multiple continueGame calls
        if (new Date().getTime() - game.getLastContinue().getTime() < 500) {
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                continueGame(game);
            }, game.getCooldownTime(), TimeUnit.SECONDS);

            game.setGameScheduler_Cooldown(future);
            return;
        }

        game.setLastContinue(new Date());

        if(game.isGuessing()){
            try {
                // Check the answers and update the leaderboard

                ConcurrentHashMap<String, TotalGuessDTO> leaderboard = new ConcurrentHashMap<>();
                List<Player> playersToUpdate = new ArrayList<>();

                int correctAnswers = checkAnswer(game);

                double guessRate = (double) beatmap.getPlaycount_answer() / (double) beatmap.getPlaycount();
                double difficultyBonus = AnswerUtil.getDifficultyBonus(guessRate);

                double poolSizeBonus = AnswerUtil.getPoolSizeBonus(game.getBeatmapsPoolSize());

                // Get leaderboard and update scores
                for (Map.Entry<Player, TotalGuessDTO> entry : game.getLeaderboard().entrySet()) {
                    TotalGuessDTO totalGuessDTO = entry.getValue();
                    if (game.getPlayerAnswers().containsKey(entry.getKey())) { // If the player has answered
                        PlayerAnswer playerAnswer = game.getPlayerAnswers().get(entry.getKey());

                        Player player = entry.getKey();
                        Long timeDiff = playerAnswer.getSubmittedTime() - game.getLastGuess().getTime();
                        double speedBonus = AnswerUtil.getSpeedBonus(timeDiff);
                        double point = 1 * speedBonus * difficultyBonus * poolSizeBonus;

                        playerAnswer.setDifficultyBonus(difficultyBonus);
                        playerAnswer.setSpeedBonus(speedBonus);
                        playerAnswer.setPoolSizeBonus(poolSizeBonus);
                        playerAnswer.setTimeTaken(timeDiff);

                        if (playerAnswer.isCorrect()) { // If the answer is correct
                            if (playerAnswer.isAliasCorrect()) point *= 0.5;

                            totalGuessDTO.setTotalGuess(totalGuessDTO.getTotalGuess() + 1);
                            totalGuessDTO.setTotalPoints(totalGuessDTO.getTotalPoints() + point);
                            totalGuessDTO.setCorrect(true);
                            totalGuessDTO.setAliasCorrect(playerAnswer.isAliasCorrect());

                            playerAnswer.setTotalPoints(point);
//                            entry.getKey().setPoints(entry.getKey().getPoints().add(BigDecimal.valueOf(point)));

                            playersToUpdate.add(player);
                        }
                    }
                    entry.setValue(totalGuessDTO);
                    leaderboard.put(entry.getKey().getUsername(), totalGuessDTO);
                }

                game.setAllAnswered(false);

                systemMessageHandler.onAnswerUpdate(game, beatmap);

                systemMessageHandler.onGameLeaderboardUpdate(game, leaderboard);

                ConcurrentHashMap<String, PlayerAnswer> answers = new ConcurrentHashMap<>();

                for (Map.Entry<Player, PlayerAnswer> entry : game.getPlayerAnswers().entrySet()) {
                    answers.put(entry.getKey().getId(), entry.getValue());
                }

                systemMessageHandler.onPlayersAnswersUpdate(game, answers);

                if (beatmap.isBlur()) {
                    String encodedId = aesUtil.encrypt(String.valueOf(beatmap.getBeatmapset_id()));
                    String encodedKey = aesUtil.encrypt(encodedId);
                    systemMessageHandler.onBlurReveal(game, encodedId, encodedKey);
                }else{
                    String encodedId = aesUtil.encrypt(String.valueOf(beatmap.getBeatmapset_id()));
                    systemMessageHandler.onBlurReveal(game, encodedId);
                }

                if (game.isRanked()) {
                    for (Player player : game.getPlayers()) {
                        PlayerAnswer playerAnswer = game.getPlayerAnswers().get(player);

                        if (playersToUpdate.contains(player)) {
                            switch (beatmap.getBeatmapDifficulty()) {
                                case EASY -> player.setMaps_guessed_easy(player.getMaps_guessed_easy() + 1);
                                case NORMAL -> player.setMaps_guessed_normal(player.getMaps_guessed_normal() + 1);
                                case HARD -> player.setMaps_guessed_hard(player.getMaps_guessed_hard() + 1);
                                case INSANE -> player.setMaps_guessed_insane(player.getMaps_guessed_insane() + 1);
                                case EXTRA -> player.setMaps_guessed_extra(player.getMaps_guessed_extra() + 1);
                            }

                            if (playerAnswer.isCorrect()) {
                                player.setPoints(player.getPoints().add(BigDecimal.valueOf(playerAnswer.getTotalPoints())));
                            }
                        }

                        saveHistoryDetail(game, player, beatmap, playerAnswer);

                        switch (beatmap.getBeatmapDifficulty()) {
                            case EASY -> player.setMaps_played_easy(player.getMaps_played_easy() + 1);
                            case NORMAL -> player.setMaps_played_normal(player.getMaps_played_normal() + 1);
                            case HARD -> player.setMaps_played_hard(player.getMaps_played_hard() + 1);
                            case INSANE -> player.setMaps_played_insane(player.getMaps_played_insane() + 1);
                            case EXTRA -> player.setMaps_played_extra(player.getMaps_played_extra() + 1);
                        }
                    }
                }

                game.getPlayerAnswers().clear();
                game.setQuestionIndex(game.getQuestionIndex() + 1);

                if (game.isRanked()) {
                    beatmapService.addPlaycount(beatmap.getBeatmapset_id(), correctAnswers, game.getPlayers().size());
                    playerRepository.saveAll(game.getPlayers());
                }

                // Check if the game is over
                if (game.getQuestionIndex() >= game.getTotalQuestions()) {
                    messageService.sendSystemMessage(game.getUuid(), "Game Over! ");
                    stopGame(game);
                    return;
                }
            } catch (Exception e) {
                logger.error("Failed to continue the game, isGuessing = true: ", e);
            }

            scheduleNextGamePhase(game, game.getCooldownTime());
        }else{
            // if isGuessing = false
            try{
                // Get the next beatmap, encode the beatmapset_id in Base64 and send it to the frontend
                sendEncodedBeatmapId(game);

            } catch (Exception e) {
                logger.error("Failed to continue the game, isGuessing = false: ", e);
            }
            game.setLastGuess(new Date());

            scheduleNextGamePhase(game, game.getGuessingTime() + 1);

            game.getPlayerAnswers().clear();
        }
        game.setGuessing(!game.isGuessing());
    }

    @Async
    void saveHistoryDetail(Game game, Player player, Beatmap beatmap, PlayerAnswer playerAnswer) {
        if(game == null || player == null || beatmap == null) return;
        try{
            if (playerAnswer == null) {
                playerAnswer = PlayerAnswer.builder()
                        .timeTaken((long) game.getGuessingTime() * 1000)
                        .isCorrect(false)
                        .build();
            }

            LobbyHistoryDetailId id = LobbyHistoryDetailId.builder()
                    .player_id(player.getId())
                    .session_id(game.getSessionId().toString())
                    .beatmapset_id(beatmap.getBeatmapset_id())
                    .build();

            double difficultyBonus = AnswerUtil.getDifficultyBonus(beatmap.getAnswer_rate());
            double speedBonus = AnswerUtil.getSpeedBonus(playerAnswer.getTimeTaken());

            LobbyHistoryDetail lobbyHistoryDetail = LobbyHistoryDetail.builder()
                    .id(id)
                    .lobbyHistory(LobbyHistory.builder().session_id(game.getSessionId().toString()).build())
                    .player(player)
                    .beatmap(beatmap)
                    .gametype(game.getGameMode().name())
                    .answer(playerAnswer.isCorrect())
                    .timetaken(playerAnswer.getTimeTaken())
                    .difficulty_bonus(difficultyBonus)
                    .speed_bonus(speedBonus)
                    .difficulty(beatmap.getBeatmapDifficulty().name())
                    .build();

            lobbyHistoryDetailRepository.save(lobbyHistoryDetail);
        } catch (Exception e) {
            logger.error("Failed to save history detail: ", e);
        }
    }

    private void scheduleNextGamePhase(Game game, int delay) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            continueGame(game);
        }, delay, TimeUnit.SECONDS);
        if (!game.isGuessing()) {
            game.setGameScheduler_Guess(future);
        } else {
            game.setGameScheduler_Cooldown(future);
        }
    }

    private void sendEncodedBeatmapId(Game game) {
        Beatmap beatmap = game.getBeatmaps().get(game.getQuestionIndex());

        resourceService.getAudio(beatmap.getBeatmapset_id());
        resourceService.getImage(beatmap.getBeatmapset_id(), beatmap.isBlur());

        if (game.getQuestionIndex() < game.getBeatmaps().size() - 1) {
            // Preload the next beatmap
            Beatmap nextBeatmap = game.getBeatmaps().get(game.getQuestionIndex() + 1);
            resourceService.getAudioAsync(nextBeatmap.getBeatmapset_id());
            resourceService.getImageAsync(nextBeatmap.getBeatmapset_id(), nextBeatmap.isBlur());
        }

        EncryptedBeatmapInfo encryptedBeatmapInfo = new EncryptedBeatmapInfo();
        String encodedId = aesUtil.encrypt(String.valueOf(beatmap.getBeatmapset_id()));
        encryptedBeatmapInfo.setBase64(encodedId);
        encryptedBeatmapInfo.setBlur(beatmap.isBlur());
        systemMessageHandler.onGameProgressUpdate(game, encryptedBeatmapInfo);
    }

    public void updateActivity(Game game) {
        if (game == null) {
            return;
        }
        game.setLastActivity(new Date());
    }

    public Game updateRoomSettings(UpdateRoomSettingsDTO updateRoomSettingsDTO) {
        UUID gameId = updateRoomSettingsDTO.getUuid();

        Game game = getGameById(gameId);
        if (game == null) {
            return null;
        }

        // Define previous settings to compare
        Game prevGame = Game.builder()
                .name(game.getName())
                .isPrivate(game.isPrivate())
                .password(game.getPassword())
                .totalQuestions(game.getTotalQuestions())
                .guessingTime(game.getGuessingTime())
                .cooldownTime(game.getCooldownTime())
                .gameMode(game.getGameMode())
                .poolMode(game.getPoolMode())
                .genreType(new ArrayList<>(game.getGenreType()))
                .languageType(new ArrayList<>(game.getLanguageType()))
                .displayMode(new ArrayList<>(game.getDisplayMode()))
                .autoskip(game.isAutoskip())
                .ranked(game.isRanked())
                .difficulty(new ArrayList<>(game.getDifficulty()))
                .startYear(game.getStartYear())
                .endYear(game.getEndYear())
                .build();

        setGameProperties(game, updateRoomSettingsDTO);

        systemMessageHandler.onRoomSettingsUpdate(game);

        // Compare the previous settings with the new settings, print what has changed
        if (!prevGame.getName().equals(game.getName())) {
            messageService.sendSystemMessage(game.getUuid(), "Room name has been changed to **" + game.getName() + "**");
        }

        if (prevGame.getTotalQuestions() != game.getTotalQuestions()) {
            messageService.sendSystemMessage(game.getUuid(), "Total questions have been changed to **" + game.getTotalQuestions() + "**");
        }

        if (prevGame.getGuessingTime() != game.getGuessingTime()) {
            messageService.sendSystemMessage(game.getUuid(), "Guessing time has been changed to **" + game.getGuessingTime() + "**");
        }

        if (prevGame.getCooldownTime() != game.getCooldownTime()) {
            messageService.sendSystemMessage(game.getUuid(), "Cooldown time has been changed to **" + game.getCooldownTime() + "**");
        }

        if (prevGame.isAutoskip() != game.isAutoskip()) {
            messageService.sendSystemMessage(game.getUuid(), "Autoskip has been " + (game.isAutoskip() ? "enabled" : "disabled"));
        }

        if (!prevGame.getDifficulty().equals(game.getDifficulty())) {
            messageService.sendSystemMessage(game.getUuid(), "Difficulty has been changed to **" + game.getDifficulty() + "**");
        }

        if (prevGame.getStartYear() != game.getStartYear() || prevGame.getEndYear() != game.getEndYear()) {
            messageService.sendSystemMessage(game.getUuid(), "Year range has been changed to **" + game.getStartYear() + " - " + game.getEndYear() + "**");
        }

        if (!prevGame.getGameMode().equals(game.getGameMode())) {
            messageService.sendSystemMessage(game.getUuid(), "Game mode has been changed to **" + game.getGameMode() + "**");
        }

        if (!prevGame.getPoolMode().equals(game.getPoolMode())) {
            messageService.sendSystemMessage(game.getUuid(), "Pool mode has been changed to **" + game.getPoolMode() + "**");
        }

        if (!prevGame.getDisplayMode().equals(game.getDisplayMode())) {
            messageService.sendSystemMessage(game.getUuid(), "Display mode has been changed to **" + game.getDisplayMode() + "**");
        }

        if (!prevGame.getGenreType().stream().filter(genre -> genre != GenreType.ANY).toList()
                .equals(game.getGenreType().stream().filter(genre -> genre != GenreType.ANY).collect(Collectors.toList()))) {
            List<GenreType> addedGenres = game.getGenreType().stream().filter(genre -> genre != GenreType.ANY).collect(Collectors.toList());
            addedGenres.removeAll(prevGame.getGenreType().stream().filter(genre -> genre != GenreType.ANY).toList());

            List<GenreType> removedGenres = prevGame.getGenreType().stream().filter(genre -> genre != GenreType.ANY).collect(Collectors.toList());
            removedGenres.removeAll(game.getGenreType().stream().filter(genre -> genre != GenreType.ANY).toList());

            if (!addedGenres.isEmpty()) {
                messageService.sendSystemMessage(game.getUuid(), "Genres added: **" + addedGenres.stream().map(Enum::name).collect(Collectors.joining(", ")) + "**");
            }
            if (!removedGenres.isEmpty()) {
                messageService.sendSystemMessage(game.getUuid(), "Genres removed: **" + removedGenres.stream().map(Enum::name).collect(Collectors.joining(", ")) + "**");
            }
        }

        if (!prevGame.getLanguageType().stream().filter(language -> language != LanguageType.ANY).toList()
                .equals(game.getLanguageType().stream().filter(language -> language != LanguageType.ANY).collect(Collectors.toList()))) {
            List<LanguageType> addedLanguages = game.getLanguageType().stream().filter(language -> language != LanguageType.ANY).collect(Collectors.toList());
            addedLanguages.removeAll(prevGame.getLanguageType().stream().filter(language -> language != LanguageType.ANY).toList());

            List<LanguageType> removedLanguages = prevGame.getLanguageType().stream().filter(language -> language != LanguageType.ANY).collect(Collectors.toList());
            removedLanguages.removeAll(game.getLanguageType().stream().filter(language -> language != LanguageType.ANY).toList());

            if (!addedLanguages.isEmpty()) {
                messageService.sendSystemMessage(game.getUuid(), "Languages added: **" + addedLanguages.stream().map(Enum::name).collect(Collectors.joining(", ")) + "**");
            }
            if (!removedLanguages.isEmpty()) {
                messageService.sendSystemMessage(game.getUuid(), "Languages removed: **" + removedLanguages.stream().map(Enum::name).collect(Collectors.joining(", ")) + "**");
            }
        }

        if (prevGame.isRanked() && !game.isRanked()) {
            messageService.sendSystemMessage(game.getUuid(), "Game settings for this lobby do not meet the requirements for ranked play. Games will be unranked and profile stats will not be updated.");
        }

        if (!prevGame.isRanked() && game.isRanked()) {
            messageService.sendSystemMessage(game.getUuid(), "Game settings for this lobby meet the requirements for ranked play. Games will be ranked and profile stats will be updated.");
        }

        String tags = "";

        if (game.getPoolMode() == PoolMode.TOUHOU) {
            tags = "touhou";
        } else if (game.getPoolMode() == PoolMode.VOCALOID) {
            tags = "vocaloid";
        }

        BeatmapPool beatmapPool = beatmapService.getBeatmapsByYearRangeAndDifficulty(game.getStartYear(), game.getEndYear(),
                game.getDifficulty(), game.getTotalQuestions(), tags,
                game.getGenreType(), game.getLanguageType());
        int totalBeatmapPoolSize = beatmapPool.getTotalBeatmapPoolSize();

        messageService.sendSystemMessage(game.getUuid(), "Estimated beatmap pool size: " + totalBeatmapPoolSize);

        return game;
    }

    private void setGameProperties(Game game, GameSettingsDTO gameSettingsDTO) {
        game.setName(gameSettingsDTO.getName());
        game.setTotalQuestions(Math.min(MAX_TOTAL_QUESTIONS, Math.max(MIN_TOTAL_QUESTIONS, gameSettingsDTO.getTotalQuestions())));
        game.setGuessingTime(Math.min(MAX_GUESSING_TIME, Math.max(MIN_GUESSING_TIME, gameSettingsDTO.getGuessingTime())));
        game.setCooldownTime(Math.min(MAX_COOLDOWN_TIME, Math.max(MIN_COOLDOWN_TIME, gameSettingsDTO.getCooldownTime())));
        game.setAutoskip(gameSettingsDTO.isAutoskip());
        game.setStartYear(Math.max(2007, gameSettingsDTO.getStartYear()));
        game.setEndYear(Math.min(Year.now().getValue(), gameSettingsDTO.getEndYear()));
        game.setPoolMode(gameSettingsDTO.getPoolMode());
        game.setGameMode(gameSettingsDTO.getGameMode());
        game.setDisplayMode(gameSettingsDTO.getDisplayMode());
        game.setGenreType(gameSettingsDTO.getGenreType());
        game.setLanguageType(gameSettingsDTO.getLanguageType());

        if(game.getStartYear() > game.getEndYear()) {
            game.setStartYear(game.getEndYear());
        }

        game.setRanked(true);

        // Ranked game should have both audio and background display modes, otherwise unranked
        // Artist and Creator (Mapper) gameModes are unranked
        if ((!game.getDisplayMode().contains(DisplayMode.AUDIO) || !game.getDisplayMode().contains(DisplayMode.BACKGROUND))
        || (game.getGameMode() == GameMode.ARTIST || game.getGameMode() == GameMode.CREATOR)) {
            game.setRanked(false);
        }

        game.getDifficulty().clear();
        game.getDifficulty().addAll(gameSettingsDTO.getDifficulty());
        game.getDifficulty().sort(Comparator.comparing(Enum::ordinal));
    }

    public List<Game> getGames() {
        return games.values().stream()
                .filter(game -> !game.isHidden())
                .collect(Collectors.toList());
    }

    public boolean startGame(Game game) {
        if (game.isPlaying() || game.isGuessing()) {
            logger.info("Game: " + game.getUuid() + " is already in play");
            return false;
        }

        messageService.sendSystemMessage(game.getUuid(), "Starting game...");

        String tags = "";

        if (game.getPoolMode() == PoolMode.TOUHOU) {
            tags = "touhou";
        } else if (game.getPoolMode() == PoolMode.VOCALOID) {
            tags = "vocaloid";
        }

        BeatmapPool beatmapPool = beatmapService.getBeatmapsByYearRangeAndDifficulty(game.getStartYear(), game.getEndYear(),
                game.getDifficulty(), game.getTotalQuestions(), tags,
                game.getGenreType(), game.getLanguageType());
        List<Beatmap> beatmaps = beatmapPool.getBeatmaps();
        int totalBeatmapPoolSize = beatmapPool.getTotalBeatmapPoolSize();

        if (totalBeatmapPoolSize < game.getTotalQuestions()) {
            // Beatmap pool sanity check
            if (totalBeatmapPoolSize < 10) {
                logger.error("Beatmap pool size should be at least 10. Found: " + beatmaps.size() + ". Game will not be started.");
                messageService.sendSystemMessage(game.getUuid(), "Beatmap pool size should be at least 10. Found: " + beatmaps.size() + ". Game will not be started.");
                return false;
            }
            game.setTotalQuestions(beatmaps.size());

            messageService.sendSystemMessage(game.getUuid(), "Not enough beatmaps found for the selected year range and difficulty. Game will be played with " + game.getTotalQuestions() + " beatmaps.");
            systemMessageHandler.onRoomSettingsUpdate(game);
        }

        game.setBeatmaps(beatmaps);
        game.setBeatmapsPoolSize(totalBeatmapPoolSize);

        double poolSizeBonus = AnswerUtil.getPoolSizeBonus(game.getBeatmapsPoolSize());

        game.setSessionId(UUID.randomUUID());

        game.setPlaying(true);
        game.setGuessing(true);
        game.setQuestionIndex(0);

        game.getLeaderboard().clear();
        game.getPlayerAnswers().clear();

        // Initialize leaderboard
        for(Player player : game.getPlayers()) {
            game.getLeaderboard().put(player, new TotalGuessDTO());
            player.setLast_played(new Date());
        }

        // Cancel the previous task if it exists
        cancelSchedulers(game);

        try{
            sendEncodedBeatmapId(game);
        }catch (Exception e) {
            logger.error("Failed to send encoded beatmapId: ", e);
        }

        game.setLastContinue(new Date());
        game.setLastGuess(new Date());

        // Schedule a new task
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            continueGame(game);
        }, game.getGuessingTime() + 1, TimeUnit.SECONDS);

        // Store the future in the game object
        game.setGameScheduler_Guess(future);

        systemMessageHandler.onGameStatusUpdate(game, "gameStart");
        logger.info("Game " + game + " has started. SessionID: " + game.getSessionId() + " | players " + game.getPlayerListAsString());

        LobbyHistory lobbyHistory = LobbyHistory.builder()
                .session_id(game.getSessionId().toString())
                .lobby_id(game.getUuid().toString())
                .time(new Date())
                .difficulties(game.getDifficulty().stream().map(Enum::name).collect(Collectors.joining(",")))
                .total_questions(game.getTotalQuestions())
                .lobby_title(game.getName())
                .host(game.getOwner())
                .guess_time(game.getGuessingTime())
                .cooldown_time(game.getCooldownTime())
                .start_year(game.getStartYear())
                .end_year(game.getEndYear())
                .poolsize_bonus(poolSizeBonus)
                .pool_mode(String.valueOf(game.getPoolMode()))
                .genre(game.getGenreType().stream().map(Enum::name).collect(Collectors.joining(",")))
                .language(game.getLanguageType().stream().map(Enum::name).collect(Collectors.joining(",")))
                .build();

        lobbyHistoryRepository.save(lobbyHistory);

        playerRepository.saveAll(game.getPlayers());

        return true;
    }

    private boolean stopGame(Game game) {
        if (game == null) {
            return false;
        }

        // If the line below (and its declaration) throws error just comment it out
        // I haven't uploaded on github to not leak the achievement details
        achievementService.checkAchievements(game);

        game.setPlaying(false);
        game.setGuessing(false);

        systemMessageHandler.onGameStatusUpdate(game, "gameEnd");

        logger.info("Game: " + game.getUuid().toString() + " has ended");

        return true;
    }

    public boolean kickPlayer(Game game, Player targetPlayer, boolean isBan) {
        if (game == null) {
            return false;
        }

        if (!game.getPlayers().contains(targetPlayer)) {
            return false;
        }

        game.getPlayers().remove(targetPlayer);

        if (isBan) {
            game.getBannedPlayers().add(targetPlayer);
            messageService.sendSystemMessage(game.getUuid(), targetPlayer.getUsername() + " has been banned from the lobby.");
            logger.info("Player: " + targetPlayer.getUsername() + " has been banned from the game " + game);
        } else {
            messageService.sendSystemMessage(game.getUuid(), targetPlayer.getUsername() + " has been kicked from the lobby.");
            logger.info("Player: " + targetPlayer.getUsername() + " has been kicked from the game " + game);
        }

        systemMessageHandler.onPlayersUpdate(game);
        systemMessageHandler.onPlayerKick(game, targetPlayer);

        return true;
    }

    public void kickInactivePlayer(Game game, Player targetPlayer) {
        if (game == null) {
            return;
        }

        if (!game.getPlayers().contains(targetPlayer)) {
            return;
        }

        game.getPlayers().remove(targetPlayer);

        logger.info("Player: " + targetPlayer.getUsername() + " has been inactive for 10 minutes. Kicking player.");

        // if the game is empty
        if (game.getPlayers().isEmpty()) {
            deleteGame(game);
            return;
        }

        // Check if the player was the owner of the game
        if (game.getOwner().getId().equals(targetPlayer.getId())) {
            // If the target player was the owner, transfer ownership to the next player
            Player newOwner = game.getPlayers().get(0);
            transferOwnership(game, newOwner);
        }

        messageService.sendSystemMessage(game.getUuid(), targetPlayer.getUsername() + " has been kicked from the game due to inactivity.");

        systemMessageHandler.onPlayersUpdate(game);
        systemMessageHandler.onPlayerInactivityKick(game, targetPlayer);
    }

    public Player transferOwnership(Game game, Player newOwner) {
        game.setOwner(newOwner);

        messageService.sendSystemMessage(game.getUuid(), newOwner.getUsername() + " is now the owner of the game.");

        systemMessageHandler.onPlayersUpdate(game);
        systemMessageHandler.onRoomSettingsUpdate(game);

        return newOwner;
    }

    public void sendAnnouncement(String content) {
        logger.info("Sending Announcement: " + content);
        for (Game game : games.values()) {
            systemMessageHandler.onAnnouncement(game, content);
        }
    }

    public LeaderboardDTO getLeaderboard() {
        List<Player> players = playerRepository.findByBanFalseOrderByPointsDesc(PageRequest.of(0, 5));
        List<Object[]> groupedDonations = donationRepository.findDonationsGroupedByPlayer();
        List<Donation> recentDonations = donationRepository.findRecentDonations();

        List<PlayerDonationDTO> donators = groupedDonations.stream()
                .map(result -> new PlayerDonationDTO((Player) result[0], (Double) result[1]))
                .sorted(Comparator.comparingDouble(PlayerDonationDTO::getTotalAmount).reversed())
                .limit(5)
                .toList();

        int totalPlayers = playerRepository.findAll().size();

        LeaderboardDTO leaderboardDTO = new LeaderboardDTO();
        leaderboardDTO.setTopPlayers(players);
        leaderboardDTO.setTopDonators(donators);
        leaderboardDTO.setRecentDonations(recentDonations);
        leaderboardDTO.setTotalItems(totalPlayers);

        return leaderboardDTO;
    }

    public LeaderboardDTO getFullLeaderboard(int page, int limit) {
        List<Player> players = playerRepository.findByBanFalseOrderByPointsDesc(PageRequest.of(page, limit));
        int totalPlayers = playerRepository.findAll().size();

        LeaderboardDTO leaderboardDTO = new LeaderboardDTO();
        leaderboardDTO.setTopPlayers(players);
        leaderboardDTO.setTotalItems(totalPlayers);

        return leaderboardDTO;
    }

    public void leaveAllGames(Game targetGame, Player player) {
        // Leave all games
        for (Game game : games.values()) {
            if (game.getPlayers().stream().anyMatch(p -> p.getId().equals(player.getId()))) {
                leaveGame(game, player);
            }
        }
    }

    public Game getGameById(UUID gameId) {
        if (!games.containsKey(gameId)) {
            return null;
        }
        return games.get(gameId);
    }

    private void cancelSchedulers(Game game) {
        if (game.getGameScheduler_Guess() != null) game.getGameScheduler_Guess().cancel(false);
        if (game.getGameScheduler_Cooldown() != null) game.getGameScheduler_Cooldown().cancel(false);
    }
}
