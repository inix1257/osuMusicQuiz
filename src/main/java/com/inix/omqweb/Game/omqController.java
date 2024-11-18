package com.inix.omqweb.Game;

import com.inix.omqweb.Announcement.AnnouncementSendDTO;
import com.inix.omqweb.DTO.*;
import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.BeatmapService;
import com.inix.omqweb.osuAPI.Player;
import com.inix.omqweb.osuAPI.PlayerRepository;
import com.inix.omqweb.osuAPI.osuAPIService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class omqController {
    private final GameManager gameManager;
    private final BeatmapService beatmapService;
    private final PlayerRepository playerRepository;

    @Value("${osu.adminUserId}")
    private String adminUserId;

    private final Logger logger = LoggerFactory.getLogger(omqController.class);

    @PostMapping("/createNewGame")
    public ResponseEntity<Game> createNewGame(@RequestBody CreateGameDTO createGameDTO, HttpServletRequest request) {
        // Get user from token
        Player player = (Player) request.getAttribute("userInfo");

        Game game = gameManager.createGame(createGameDTO, player);

        return ResponseEntity.ok(game);
    }

    @GetMapping("/gamerooms")
    public ResponseEntity<?> getGamerooms() {
        return ResponseEntity.ok(gameManager.getGames());
    }

    @PostMapping("/joinGame")
    public ResponseEntity<Game> joinGame(@RequestBody JoinGameDTO joinGameDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        switch (gameManager.joinGame(joinGameDTO, player)) {
            case Game.GAME_JOIN_STATUS_SUCCESS:
                return ResponseEntity.ok(gameManager.getGameById(UUID.fromString(joinGameDTO.getGameId())));
            case Game.GAME_JOIN_STATUS_NOT_EXIST:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            case Game.GAME_JOIN_STATUS_USER_ALREADY_EXISTS, Game.GAME_JOIN_STATUS_USER_ALREADY_IN_ANOTHER_GAME:
                return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
            case Game.GAME_JOIN_STATUS_GAME_IS_PLAYING:
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            case Game.GAME_JOIN_STATUS_INCORRECT_PASSWORD:
                return ResponseEntity.status(HttpStatus.LOCKED).body(null);
            case Game.GAME_JOIN_STATUS_INVALID_TOKEN:
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            case Game.GAME_JOIN_STATUS_USER_IS_BANNED:
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(null);
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Profile("dev")
    @PostMapping("/joinGameLegacy")
    public void joinGameLegacy(@RequestBody JoinGameLegacyDTO joinGameLegacyDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        if (!player.getId().equals(adminUserId)) {
            return;
        }

        gameManager.joinGameLegacy(joinGameLegacyDTO);
    }

    @PostMapping("/leaveGame")
    public void leaveGame(@RequestBody JoinGameDTO joinGameDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");
        Game game = gameManager.getGameById(UUID.fromString(joinGameDTO.getGameId()));

        if (game == null) {
            return;
        }

        gameManager.leaveGame(game, player);
    }

    @PostMapping("/deleteGame")
    public void deleteGame(@RequestBody JoinGameDTO joinGameDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");
        Game game = gameManager.getGameById(UUID.fromString(joinGameDTO.getGameId()));

        if (game == null) {
            return;
        }

        if (!game.getOwner().equals(player)) {
            if (!player.getId().equals(adminUserId)) {
                return;
            }
        }

        gameManager.deleteGame(game);
    }

    @PostMapping("/startGame")
    public ResponseEntity<?> startGame(@RequestBody JoinGameDTO joinGameDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");
        Game game = gameManager.getGameById(UUID.fromString(joinGameDTO.getGameId()));

        if (game == null || !game.getOwner().equals(player)) {
            logger.error("User: " + player.getUsername() + " is not the owner of the game " + game.getUuid() + " or the game does not exist");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        if (gameManager.startGame(game)) {
            return ResponseEntity.ok(game);
        } else {
            logger.error("Game " + game.getUuid() + " failed to start");
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
    }

    @PostMapping("/kickPlayer")
    public void kickPlayer(@RequestBody PlayerTargetDTO playerTargetDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");
        Game game = gameManager.getGameById(UUID.fromString(playerTargetDTO.getGameId()));

        if (game == null || !game.getOwner().equals(player) && !player.getId().equals(adminUserId)) {
            throw new RuntimeException("User: " + player.getUsername() + " is not the owner of the game " + game.getUuid());
        }

        Player targetPlayer = playerRepository.findById(playerTargetDTO.getTargetUserId()).orElseThrow();

        gameManager.kickPlayer(game, targetPlayer, false);
    }

    @PostMapping("/banPlayer")
    public void banPlayer(@RequestBody PlayerTargetDTO playerTargetDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");
        Game game = gameManager.getGameById(UUID.fromString(playerTargetDTO.getGameId()));

        if (game == null || !game.getOwner().equals(player) && !player.getId().equals(adminUserId)) {
            throw new RuntimeException("User: " + player.getUsername() + " is not the owner of the game " + game.getUuid());
        }

        Player targetPlayer = playerRepository.findById(playerTargetDTO.getTargetUserId()).orElseThrow();

        gameManager.kickPlayer(game, targetPlayer, true);
    }

    @PostMapping("/transferHost")
    public void transferHost(@RequestBody PlayerTargetDTO playerTargetDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");
        Game game = gameManager.getGameById(UUID.fromString(playerTargetDTO.getGameId()));

        if (game == null || !game.getOwner().equals(player)) {
            throw new RuntimeException("User: " + player.getUsername() + " is not the owner of the game " + game.getUuid());
        }

        Player newOwner = playerRepository.findById(playerTargetDTO.getTargetUserId()).orElse(null);

        if (newOwner == null) {
            throw new RuntimeException("User with ID: " + playerTargetDTO.getTargetUserId() + " does not exist");
        }

        gameManager.transferOwnership(game, newOwner);
    }

    @GetMapping("/user")
    public ResponseEntity<Player> getUser(@RequestParam String id) {
        Player player = playerRepository.findById(id).orElse(null);

        if (player == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        player.setRank(playerRepository.countByPointsGreaterThanAndBanFalse(player.getPoints()) + 1);

        return ResponseEntity.ok(player);
    }

    @PostMapping("/updateRoomSettings")
    public ResponseEntity<Game> updateRoomSettings(@RequestBody UpdateRoomSettingsDTO updateRoomSettingsDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");
        Game game = gameManager.getGameById(updateRoomSettingsDTO.getUuid());

        if (game == null || !game.getOwner().equals(player)) {
            throw new RuntimeException("User: " + player.getUsername() + " is not the owner of the game " + game.getUuid());
        }

        return ResponseEntity.ok(gameManager.updateRoomSettings(updateRoomSettingsDTO));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard() {
        return ResponseEntity.ok(gameManager.getLeaderboard());
    }

    @GetMapping("/fullleaderboard")
    public ResponseEntity<?> getFullLeaderboard(@RequestParam int page, @RequestParam int limit) {
        return ResponseEntity.ok(gameManager.getFullLeaderboard(page, limit));
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getBeatmapCount() {
        int beatmapCount = beatmapService.getBeatmapCount();
        return ResponseEntity.ok(beatmapCount);
    }

    @GetMapping("/possibleAnswers")
    public ResponseEntity<?> getPossibleAnswers() {
        return ResponseEntity.ok(beatmapService.getPossibleAnswers());
    }

    @GetMapping("/possibleAnswers_artist")
    public ResponseEntity<?> getPossibleAnswers_artist() {
        return ResponseEntity.ok(beatmapService.getPossibleArtists());
    }

    @GetMapping("/possibleAnswers_creator")
    public ResponseEntity<?> getPossibleAnswers_creator() {
        return ResponseEntity.ok(beatmapService.getPossibleCreators());
    }

    @PostMapping("/addBeatmap")
    public ResponseEntity<Beatmap> addBeatmap(@RequestBody BeatmapAddDTO beatmapAddDTO, HttpServletRequest request) throws IOException, ParseException {
        Player player = (Player) request.getAttribute("userInfo");

        if (!player.getId().equals(adminUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        return ResponseEntity.ok(beatmapService.addBeatmap(beatmapAddDTO.getBeatmapsetId()));
    }

    @PostMapping("/ingameAnnouncement")
    public void sendAnnouncement(@RequestBody AnnouncementSendDTO announcementSendDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        if (!player.getId().equals(adminUserId)) {
            return;
        }

        gameManager.sendAnnouncement(announcementSendDTO.getContent());
    }
}
