package com.inix.omqweb.Beatmap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inix.omqweb.Beatmap.Alias.*;
import com.inix.omqweb.BeatmapReport.*;
import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Game.GameMode;
import com.inix.omqweb.Game.GuessMode;
import com.inix.omqweb.Game.ResourceService;
import com.inix.omqweb.Util.AESUtil;
import com.inix.omqweb.Util.DifficultyCalc;
import com.inix.omqweb.osuAPI.PlayerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BeatmapService {
    private final BeatmapRepository beatmapRepository;
    private final PlayerRepository playerRepository;
    private final BeatmapReportRepository beatmapReportRepository;
    private final BeatmapPatternRepository beatmapPatternRepository;
    private final ResourceService resourceService;
    private final AliasRepository aliasRepository;
    private final AliasNewRepository aliasNewRepository;
    private final AliasNewService aliasNewService;
    private final AESUtil aesUtil;

    private final Logger logger = LoggerFactory.getLogger(BeatmapService.class);

    @Value("${osu.apiKey}")
    private String apiKey;

//    @PostConstruct
    private void fetchBeatmaps() {
        String filePath = "./parse.txt";

        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.forEach(line -> {
                System.out.println("Fetching data for beatmapset_id: " + line);
                resourceService.getImage(Integer.parseInt(line), true);
//                resourceService.getAudio(Integer.parseInt(line));
//                addBlur(line);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Finished fetching beatmaps");
    }

//    @Profile("dev")
//    @Scheduled(fixedDelay = 1000)
    private void fetchTags() throws IOException {
        List<Beatmap> beatmaps = beatmapRepository.findRandomBeatmapsWithNoTags();

        for (Beatmap beatmap : beatmaps) {
            System.out.println("Fetching data for beatmapset_id: " + beatmap.getBeatmapset_id() + " " + beatmap.getArtistAndTitle());

            URL url = new URL("https://osu.ppy.sh/api/get_beatmaps?k=" + apiKey + "&s=" + beatmap.getBeatmapset_id());
            BufferedReader bf; String line = ""; String result="";
            bf = new BufferedReader(new InputStreamReader(url.openStream()));

            while((line=bf.readLine())!=null){
                result=result.concat(line);
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonMap = mapper.readValue(result.substring(1, result.length() - 1), new TypeReference<Map<String, String>>() {
            });

            String title = jsonMap.get("title");
            String artist = jsonMap.get("artist");
            String tags = jsonMap.get("tags");
            String source = jsonMap.get("source");
            String genre = jsonMap.get("genre_id");
            String language = jsonMap.get("language_id");

            List<String> tagList = getTags(artist, title, source, tags);

            // Convert the list of tags to a single string
            String tagString = String.join(" ", tagList);
            beatmap.setTags(tagString);
            beatmap.setGenre(genre);
            beatmap.setLanguage(language);

            System.out.println("Tags: " + tagString);

            if (!tagString.isEmpty()) {
                System.out.println("Adding beatmap to save list: " + beatmap.getArtistAndTitle());
            } else {
                // Save empty tag
            }
        }

        System.out.println("Saved: " + beatmapRepository.saveAll(beatmaps).size());
    }

    @PostConstruct
    @Scheduled(fixedDelay = 1000 * 60 * 60) // Refresh every hour
    public void updateDifficultyRanges() {
        for (GuessMode guessMode : GuessMode.values()) {
            List<Beatmap> beatmaps = beatmapRepository.findBeatmapsByAnswerRate(guessMode.toString());

            int totalBeatmaps = beatmaps.size();

            int diffIndex_easy = (int) (totalBeatmaps * 0.1);
            int diffIndex_normal = (int) (totalBeatmaps * 0.3);
            int diffIndex_hard = (int) (totalBeatmaps * 0.6);
            int diffIndex_insane = (int) (totalBeatmaps * 0.9);

            double[] ranges = new double[GameDifficulty.values().length + 1];

            ranges[0] = 1.0;
            ranges[1] = beatmaps.get(diffIndex_easy).getBeatmapStatsDTO().get(guessMode.getValue()).getGuess_rate();
            ranges[2] = beatmaps.get(diffIndex_normal).getBeatmapStatsDTO().get(guessMode.getValue()).getGuess_rate();
            ranges[3] = beatmaps.get(diffIndex_hard).getBeatmapStatsDTO().get(guessMode.getValue()).getGuess_rate();
            ranges[4] = beatmaps.get(diffIndex_insane).getBeatmapStatsDTO().get(guessMode.getValue()).getGuess_rate();
            ranges[5] = 0.0;

            DifficultyCalc.DIFF_BARS[guessMode.getValue()] = ranges;

            logger.info("Difficulty ranges for " + guessMode + ": " + Arrays.toString(ranges));
        }
    }

    public BeatmapPool getBeatmapsWithSettings(int startYear, int endYear, List<GameDifficulty> difficulties, int limit, String tag, List<GenreType> genres, List<LanguageType> languages, GameMode gameMode, GuessMode guessMode) {
        if (startYear > endYear) endYear = startYear;

        Calendar calendar = Calendar.getInstance();
        calendar.clear();

        // Convert start year to Timestamp
        calendar.set(Calendar.YEAR, startYear);
        Timestamp startDate = new Timestamp(calendar.getTimeInMillis());

        // Convert end year to Timestamp
        calendar.set(Calendar.YEAR, endYear + 1);
        Timestamp endDate = new Timestamp(calendar.getTimeInMillis());

        // Determine difficulty range
        double[][] difficultyRange = getDifficultyRanges(guessMode, difficulties);

        List<Beatmap> beatmaps = beatmapRepository.findBeatmapsByApprovedDateRangeAndDifficultyAndGuessMode(startDate, endDate,
                difficultyRange[0][1], difficultyRange[0][0],
                difficultyRange[1][1], difficultyRange[1][0],
                difficultyRange[2][1], difficultyRange[2][0],
                difficultyRange[3][1], difficultyRange[3][0],
                difficultyRange[4][1], difficultyRange[4][0],
                limit, "%" + tag + "%",
                genres.stream().map(GenreType::getValue).toList(),
                languages.stream().map(LanguageType::getValue).toList(), gameMode.toString(),
                guessMode.toString());

        // Using AliasNew instead of old Alias system
        if (guessMode == GuessMode.ARTIST || guessMode == GuessMode.TITLE || guessMode == GuessMode.CREATOR || guessMode == GuessMode.PATTERN) {
            // Get all relevant aliases
            List<AliasNew> newAliases = aliasNewRepository.findByAliasType(
                guessMode == GuessMode.ARTIST ? AliasType.ARTIST :
                guessMode == GuessMode.TITLE ? AliasType.TITLE :
                AliasType.CREATOR
            );
            
            // Get all unique target texts from our beatmaps
            Set<String> availableTargets = beatmaps.stream()
                .map(beatmap -> switch (guessMode) {
                    case ARTIST -> beatmap.getArtist().toLowerCase();
                    case TITLE, PATTERN -> beatmap.getTitle().toLowerCase();
                    case CREATOR -> beatmap.getCreator().toLowerCase();
                    default -> throw new IllegalArgumentException("Invalid guess mode for aliases: " + guessMode);
                })
                .collect(Collectors.toSet());
            
            // Filter aliases to only include those that match our beatmaps
            Map<String, List<String>> aliasMap = new HashMap<>();
            for (AliasNew alias : newAliases) {
                String targetText = alias.getTargetText().toLowerCase();
                if (availableTargets.contains(targetText)) {
                    aliasMap.computeIfAbsent(targetText, k -> new ArrayList<>())
                           .add(alias.getSourceText().toLowerCase());
                }
            }
            
            // Cache these for checking against user answers
            for (Beatmap beatmap : beatmaps) {
                beatmap.setAliasMap(aliasMap);
            }
        }

        if (guessMode == GuessMode.PATTERN) {
            List<Integer> beatmapIds = beatmaps.stream().map(Beatmap::getBeatmapset_id).toList();
            List<BeatmapPattern> beatmapPatterns = beatmapPatternRepository.findAllByBeatmapIds(beatmapIds);
            Map<Integer, List<BeatmapPattern>> beatmapPatternsMap = beatmapPatterns.stream()
                    .collect(Collectors.groupingBy(pattern -> pattern.getBeatmap().getBeatmapset_id()));
            beatmaps.forEach(beatmap -> beatmap.setBeatmapPattern(beatmapPatternsMap.get(beatmap.getBeatmapset_id()).stream().findFirst().orElse(null)));
        }

        BeatmapPool beatmapPool = new BeatmapPool();
        beatmapPool.setBeatmaps(beatmaps);
        beatmapPool.setTotalBeatmapPoolSize(beatmaps.size());

        return beatmapPool;
    }

    public double[][] getDifficultyRanges(GuessMode guessMode, List<GameDifficulty> difficulties) {
        double[][] ranges = new double[6][2];

        // Get the difficulty ranges for the specified guess mode
        double[] diffRanges = DifficultyCalc.DIFF_BARS[guessMode.getValue()];

        // Fill the first 4 elements with the difficulty ranges
        for (int i = 0; i < difficulties.size(); i++) {
            ranges[i] = new double[]{diffRanges[difficulties.get(i).getValue()], diffRanges[difficulties.get(i).getValue() + 1]};
        }

        // Fill the remaining elements with [-1.0, -1.0] to indicate that they are not used
        for (int i = difficulties.size(); i < 6; i++) {
            ranges[i] = new double[]{-1.0, -1.0};
        }

        return ranges;
    }

    @Cacheable(value = "possibleAnswers")
    public List<String> getPossibleAnswers(GameMode gameMode, GuessMode guessMode) {
        if (gameMode == GameMode.ANY) {
            return switch (guessMode) {
                case ARTIST -> beatmapRepository.findDistinctArtists();
                case CREATOR -> beatmapRepository.findDistinctCreators();
                default -> beatmapRepository.findDistinctTitles();
            };
        }

        return switch (guessMode) {
            case ARTIST -> beatmapRepository.findDistinctArtists(gameMode.toString());
            case CREATOR -> beatmapRepository.findDistinctCreators(gameMode.toString());
            default -> beatmapRepository.findDistinctTitles(gameMode.toString());
        };
    }

    @Async
    public void addPlaycount(GameMode gameMode, GuessMode guessMode, int beatmapsetId, int answer_playcount, int total_playcount) {
        Beatmap beatmap = beatmapRepository.findById(beatmapsetId).orElse(null);

        if (beatmap == null) {
            return;
        }

        BeatmapStats beatmapStats = beatmap.getBeatmapStats().get(new BeatmapStatsId(beatmapsetId, gameMode, guessMode));

        if (beatmapStats == null) {
            beatmapStats = BeatmapStats.builder()
                    .id(new BeatmapStatsId(beatmapsetId, gameMode, guessMode))
                    .guessed(0)
                    .played(0)
                    .beatmap(beatmap)
                    .build();
        }

        beatmapStats.setGuessed(beatmapStats.getGuessed() + answer_playcount);
        beatmapStats.setPlayed(beatmapStats.getPlayed() + total_playcount);

        beatmapRepository.save(beatmap);
    }

    @Cacheable(value = "beatmapCount")
    public int getBeatmapCount() {
        return (int) beatmapRepository.count();
    }

    public void reportBeatmap(BeatmapReportDTO beatmapReportDTO) {
        int beatmapsetId = Integer.parseInt(aesUtil.decrypt(beatmapReportDTO.getEncryptedBeatmapsetId()));

        BeatmapReport beatmapReport = BeatmapReport.builder()
                .player(playerRepository.findById(beatmapReportDTO.getUserId()).orElse(null))
                .beatmap(beatmapRepository.findById(beatmapsetId).orElse(null))
                .beatmapReportType(beatmapReportDTO.getBeatmapReportType())
                .build();

        beatmapReportRepository.save(beatmapReport);
    }

    @CacheEvict(value = {"beatmapCount", "possibleAnswers"}, allEntries = true)
    public Beatmap addBeatmap(String beatsetmap_id) throws IOException, ParseException {
        URL url = new URL("https://osu.ppy.sh/api/get_beatmaps?k=" + apiKey + "&s=" + beatsetmap_id);
        BufferedReader bf; String line; String result="";
        bf = new BufferedReader(new InputStreamReader(url.openStream()));

        while((line=bf.readLine())!=null){
            result=result.concat(line);
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> jsonList = mapper.readValue(result, new TypeReference<List<Map<String, String>>>() {});

        Map<String, String> jsonMap = jsonList.get(0);

        Set<GameMode> gameModes = new HashSet<>();

        for (Map<String, String> jm : jsonList) {
            gameModes.add(GameMode.fromValue(Integer.parseInt(jm.get("mode"))));
        }

        Beatmap existingBeatmap = beatmapRepository.findById(Integer.parseInt(jsonMap.get("beatmapset_id"))).orElse(null);
        Map<BeatmapStatsId, BeatmapStats> beatmapStatsMap = new HashMap<>();

        // Check if the beatmap already exists
        if (existingBeatmap != null) {
            beatmapStatsMap = existingBeatmap.getBeatmapStats();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<String> tagList = getTags(jsonMap.get("artist"), jsonMap.get("title"), jsonMap.get("source"), jsonMap.get("tags"));

        // Convert the list of tags to a single string
        String tagString = String.join(" ", tagList);

        for (GameMode gameMode : gameModes) {
            for (GuessMode guessMode : GuessMode.values()) {
                if (guessMode == GuessMode.PATTERN) continue;

                BeatmapStatsId beatmapStatsId = new BeatmapStatsId(Integer.parseInt(jsonMap.get("beatmapset_id")), gameMode, guessMode);

                if (beatmapStatsMap.containsKey(beatmapStatsId)) {
                    continue;
                }

                BeatmapStats beatmapStats = BeatmapStats.builder()
                        .id(beatmapStatsId)
                        .guessed(0)
                        .played(0)
                        .build();

                beatmapStatsMap.put(beatmapStatsId, beatmapStats);
            }
        }

        String title = jsonMap.get("title");
        title = title.replace(" (TV Size)", "");
        title = title.replace(" (Short Ver.)", "");
        title = title.replace(" (Cut Ver.)", "");

        Beatmap beatmap = Beatmap.builder()
                .beatmapStats(beatmapStatsMap)
                .beatmapset_id(Integer.parseInt(jsonMap.get("beatmapset_id")))
                .artist(existingBeatmap != null ? existingBeatmap.getArtist() : jsonMap.get("artist"))
                .title(existingBeatmap != null ? existingBeatmap.getTitle() : title)
                .creator(jsonMap.get("creator"))
                .language(jsonMap.get("language_id"))
                .genre(jsonMap.get("genre_id"))
                .tags(existingBeatmap != null ? existingBeatmap.getTags() : tagString)
                .approved_date(new Timestamp(dateFormat.parse(jsonMap.get("approved_date")).getTime()))
                .blur(existingBeatmap != null && existingBeatmap.isBlur())
                .deleted(existingBeatmap != null && existingBeatmap.isDeleted())
                .build();

        logger.info("Adding beatmap: " + beatmap.getArtistAndTitle() + " for gamemode: " + gameModes);

        return beatmapRepository.save(beatmap);
    }

    public Beatmap addBeatmapPattern(String beatmapId) throws IOException {
        URL url = new URL("https://osu.ppy.sh/api/get_beatmaps?k=" + apiKey + "&b=" + beatmapId);
        BufferedReader bf; String line; String result="";
        bf = new BufferedReader(new InputStreamReader(url.openStream()));

        while((line=bf.readLine())!=null){
            result=result.concat(line);
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> jsonList = mapper.readValue(result, new TypeReference<List<Map<String, String>>>() {});

        Map<String, String> jsonMap = jsonList.get(0);

        Set<GameMode> gameModes = new HashSet<>();

        for (Map<String, String> jm : jsonList) {
            gameModes.add(GameMode.fromValue(Integer.parseInt(jm.get("mode"))));
        }

        Beatmap existingBeatmap = beatmapRepository.findById(Integer.parseInt(jsonMap.get("beatmapset_id"))).orElse(null);
        Map<BeatmapStatsId, BeatmapStats> beatmapStatsMap = new HashMap<>();

        // Check if the beatmap already exists
        if (existingBeatmap != null) {
            beatmapStatsMap = existingBeatmap.getBeatmapStats();
        } else {
            return null;
        }

        BeatmapStatsId beatmapStatsId = new BeatmapStatsId(Integer.parseInt(jsonMap.get("beatmapset_id")), GameMode.STD, GuessMode.PATTERN);

        BeatmapStats beatmapStats = BeatmapStats.builder()
                .id(beatmapStatsId)
                .guessed(0)
                .played(0)
                .build();

        if (!beatmapStatsMap.containsKey(beatmapStatsId)) {
            beatmapStatsMap.put(beatmapStatsId, beatmapStats);
        }

        BeatmapPattern beatmapPattern = BeatmapPattern.builder()
                .beatmap(existingBeatmap)
                .beatmap_id(Integer.parseInt(jsonMap.get("beatmap_id")))
                .version(jsonMap.get("version"))
                .build();

        beatmapPatternRepository.save(beatmapPattern);
        return beatmapRepository.save(existingBeatmap);
    }

    public Beatmap addBlur(String beatmapset_id) {
        Beatmap beatmap = beatmapRepository.findById(Integer.parseInt(beatmapset_id)).orElse(null);
        if (beatmap == null) {
            logger.error("Beatmap not found: " + beatmapset_id);
            return null;
        }

        List<BeatmapReport> beatmapReport = beatmapReportRepository.findAllByBeatmapsetId(Integer.parseInt(beatmapset_id));
        beatmapReportRepository.deleteAll(beatmapReport);

        resourceService.forceGetImage(Integer.parseInt(beatmapset_id), true);

        if (beatmap.isBlur()) return null;

        beatmap.setBlur(true);

        logger.info("Adding blur to beatmap: " + beatmap.getId() + " / " + beatmap.getArtistAndTitle());

        return beatmapRepository.save(beatmap);
    }

    private List<String> getTags(String artist, String title, String source, String tags) {
        List<String> tagList = new ArrayList<>();

        if (tags.toLowerCase().contains("touhou") || tags.toLowerCase().contains("東方") || source.toLowerCase().contains("touhou") || source.toLowerCase().contains("東方")) {
            tagList.add("touhou");
        }

        if (tags.toLowerCase().contains("vocaloid") || tags.toLowerCase().contains("ボーカロイド") || source.toLowerCase().contains("vocaloid") || source.toLowerCase().contains("ボーカロイド")
                || artist.toLowerCase().contains("hatsune miku") || title.toLowerCase().contains("hatsune miku")
                || artist.toLowerCase().contains("kagamine rin") || title.toLowerCase().contains("kagamine rin")
                || artist.toLowerCase().contains("kagamine len") || title.toLowerCase().contains("kagamine len")
                || artist.toLowerCase().contains("megurine luka") || title.toLowerCase().contains("megurine luka")
                || artist.contains("GUMI") || title.contains("GUMI")
                || artist.toLowerCase().contains("megpoid gumi") || title.toLowerCase().contains("megpoid gumi")
        ) {
            tagList.add("vocaloid");
        }

        return tagList;
    }

    public Beatmap getBeatmapById(int beatmapsetId) {
        return beatmapRepository.findById(beatmapsetId).orElse(null);
    }

    public List<BeatmapReportResponseDTO> getMostReportedBeatmaps() {
        List<BeatmapReportProjection> projections = beatmapReportRepository.findMostReportedBeatmaps();
        return projections.stream()
                .map(projection -> {
                    BeatmapReportResponseDTO dto = new BeatmapReportResponseDTO();
                    dto.setBeatmapsetId(projection.getBeatmapsetId());
                    dto.setReportCount(projection.getReportCount());
                    dto.setBlur(projection.isBlur());
                    dto.setArtist(projection.getArtist());
                    dto.setTitle(projection.getTitle());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Beatmap deleteReport(String beatmapsetId) {
        List<BeatmapReport> beatmapReports = beatmapReportRepository.findAllByBeatmapsetId(Integer.parseInt(beatmapsetId));
        beatmapReportRepository.deleteAll(beatmapReports);

        logger.info("Deleted reports for beatmap: " + beatmapsetId);

        return beatmapReports.get(0).getBeatmap();
    }

    public void addAlias(AliasAddDTO aliasAddDTO) {
        // Create a new DTO for the new alias system
        AliasNewAddDTO newAliasDTO = AliasNewAddDTO.builder()
                .sourceText(aliasAddDTO.getSource())
                .targetText(aliasAddDTO.getTarget())
                .type(aliasAddDTO.getType())
                .build();
        
        // Add the alias using the new service
        aliasNewService.addAlias(newAliasDTO);
    }
    
    /**
     * Legacy method for adding aliases to the old system.
     * Keep this method during the transition period.
     */
    private void addAliasLegacy(AliasAddDTO aliasAddDTO) {
        List<Beatmap> target = null, source = null;

        switch (aliasAddDTO.getType()) {
            case "ARTIST":
                target = beatmapRepository.findBeatmapsByArtist(aliasAddDTO.getTarget());
                source = beatmapRepository.findBeatmapsByArtist(aliasAddDTO.getSource());
                break;
            case "TITLE":
                target = beatmapRepository.findBeatmapsByTitle(aliasAddDTO.getTarget());
                source = beatmapRepository.findBeatmapsByTitle(aliasAddDTO.getSource());
                break;
            case "CREATOR":
                target = beatmapRepository.findBeatmapsByCreator(aliasAddDTO.getTarget());
                source = beatmapRepository.findBeatmapsByCreator(aliasAddDTO.getSource());
                break;
        }

        if (target == null || source == null) {
            logger.error("Invalid alias: " + aliasAddDTO);
            return;
        }

        // Fetch all existing aliases for the relevant beatmaps
        List<Integer> sourceBeatmapIds = source.stream().map(Beatmap::getBeatmapset_id).toList();
        List<Alias> existingAliases = aliasRepository.findAllByBeatmapIds(sourceBeatmapIds);

        // Create a set of existing alias keys for quick lookup
        Set<String> existingAliasKeys = existingAliases.stream()
                .map(alias -> alias.getBeatmap().getBeatmapset_id() + "_" + alias.getAliasType() + "_" + alias.getName())
                .collect(Collectors.toSet());

        Set<Alias> aliasesToSave = new HashSet<>();

        for (Beatmap targetBeatmap : target) {
            for (Beatmap sourceBeatmap : source) {
                String aliasName = switch (aliasAddDTO.getType()) {
                    case "ARTIST" -> targetBeatmap.getArtist();
                    case "TITLE" -> targetBeatmap.getTitle();
                    case "CREATOR" -> targetBeatmap.getCreator();
                    default -> throw new IllegalArgumentException("Invalid alias type: " + aliasAddDTO.getType());
                };

                if (Objects.equals(aliasName, sourceBeatmap.getArtist()) || Objects.equals(aliasName, sourceBeatmap.getTitle()) || Objects.equals(aliasName, sourceBeatmap.getCreator())) {
                    continue;
                }

                AliasId aliasId = new AliasId(sourceBeatmap.getBeatmapset_id(), aliasName, AliasType.valueOf(aliasAddDTO.getType()).getValue());
                Alias alias = Alias.builder()
                        .aliasId(aliasId)
                        .beatmap(sourceBeatmap)
                        .build();

                aliasesToSave.add(alias);
            }
        }

        aliasRepository.saveAll(aliasesToSave);

        logger.info("Added " + aliasesToSave.size() + " aliases for " + aliasAddDTO);
    }
}
