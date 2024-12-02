package com.inix.omqweb.Game;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.GenreType;
import com.inix.omqweb.Beatmap.LanguageType;
import com.inix.omqweb.DTO.TotalGuessDTO;
import com.inix.omqweb.osuAPI.Player;
import lombok.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {
    private UUID uuid;

    @JsonIgnore
    private UUID sessionId;

    private String name;
    private Player owner;

    @JsonIgnore
    private String password;
    private boolean isPrivate;
    @JsonIgnore
    private boolean isHidden;

    private Date creationDate;
    private int questionIndex;
    private int totalQuestions;

    private int guessingTime;
    private int cooldownTime;

    private boolean autoskip;

    private List<GameDifficulty> difficulty = new ArrayList<>();
    private GameMode gameMode = GameMode.STD;
    private GuessMode guessMode;
    private List<DisplayMode> displayMode;
    private boolean isPlaying;
    private boolean isGuessing;

    private boolean ranked;

    private Date lastActivity;
    private Date lastContinue;
    private Date lastGuess;

    @JsonIgnore
    private List<Beatmap> beatmaps;
    @JsonIgnore
    private int beatmapsPoolSize;

    private CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    private List<Player> bannedPlayers = new ArrayList<>();
    private ConcurrentHashMap<Player, PlayerAnswer> playerAnswers = new ConcurrentHashMap<>();

    @JsonIgnore
    private boolean allAnswered = false;

    @JsonAnyGetter
    private ConcurrentHashMap<Player, TotalGuessDTO> leaderboard = new ConcurrentHashMap<>();

    private int startYear;
    private int endYear;

    private PoolMode poolMode;

    private List<GenreType> genreType;
    private List<LanguageType> languageType;

    @JsonIgnore
    private ScheduledFuture<?> gameScheduler_Guess;

    @JsonIgnore
    private ScheduledFuture<?> gameScheduler_Cooldown;

    public static final int GAME_JOIN_STATUS_SUCCESS = 1;

    public static final int GAME_JOIN_STATUS_NOT_EXIST = -1;
    public static final int GAME_JOIN_STATUS_USER_ALREADY_EXISTS = -2;
    public static final int GAME_JOIN_STATUS_GAME_IS_PLAYING = -3;
    public static final int GAME_JOIN_STATUS_INCORRECT_PASSWORD = -4;
    public static final int GAME_JOIN_STATUS_INVALID_TOKEN = -5;
    public static final int GAME_JOIN_STATUS_USER_ALREADY_IN_ANOTHER_GAME = -6;
    public static final int GAME_JOIN_STATUS_USER_IS_BANNED = -7;

    @Override
    public String toString() {
        return "Game{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", owner=" + owner.getUsername() +
                '}';
    }

    @JsonIgnore
    public String getPlayerListAsString() {
        StringBuilder sb = new StringBuilder();
        for (Player player : players) {
            sb.append(player.getUsername()).append(", ");
        }
        return sb.toString();
    }
}
