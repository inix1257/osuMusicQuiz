package com.inix.omqweb.Beatmap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Game.ResourceService;
import com.inix.omqweb.Util.AESUtil;
import com.inix.omqweb.Util.DifficultyCalc;
import com.inix.omqweb.osuAPI.PlayerRepository;
import com.inix.omqweb.BeatmapReport.BeatmapReport;
import com.inix.omqweb.BeatmapReport.BeatmapReportDTO;
import com.inix.omqweb.BeatmapReport.BeatmapReportRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BeatmapService {
    private final BeatmapRepository beatmapRepository;
    private final PlayerRepository playerRepository;
    private final BeatmapReportRepository beatmapReportRepository;
    private final ResourceService resourceService;
    private final AESUtil aesUtil;

    @Value("${osu.apiKey}")
    private String apiKey;

//    @PostConstruct
//    @Profile("dev")
    private void fetchBeatmaps() {
        String filePath = "./parse.txt";

        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.forEach(line -> {
                System.out.println("Fetching data for beatmapset_id: " + line);
                resourceService.getImage(Integer.parseInt(line), true);
//                resourceService.getAudio(Integer.parseInt(line));
                addBlur(line);
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

//    @PostConstruct
//    @CacheEvict(value = {"possibleAnswers"}, allEntries = true)
    public void fixMarkers() {
        List<Beatmap> beatmaps = beatmapRepository.findRandomBeatmapsWithMarkers();

        for (Beatmap beatmap : beatmaps) {
            if (beatmap.getTitle().contains(" (TV Size)")) {
                beatmap.setTitle(beatmap.getTitle().replace(" (TV Size)", ""));
            }
        }

        beatmapRepository.saveAll(beatmaps);

        System.out.println("Done removing markers");
    }

    @PostConstruct
    @Scheduled(fixedDelay = 60000) // Refresh every minute
    public double[] getDifficultyRanges() {
        List<Beatmap> beatmaps = beatmapRepository.findBeatmapsByAnswerRate();
        int totalBeatmaps = beatmaps.size();

        int firstQuartileIndex = totalBeatmaps / 4;
        int secondQuartileIndex = totalBeatmaps / 2;
        int thirdQuartileIndex = firstQuartileIndex + secondQuartileIndex;

        double[] ranges = new double[3];

        ranges[0] = beatmaps.get(firstQuartileIndex).getAnswer_rate();
        ranges[1] = beatmaps.get(secondQuartileIndex).getAnswer_rate();
        ranges[2] = beatmaps.get(thirdQuartileIndex).getAnswer_rate();

        DifficultyCalc.DIFF_EASY_BAR = ranges[0];
        DifficultyCalc.DIFF_NORMAL_BAR = ranges[1];
        DifficultyCalc.DIFF_HARD_BAR = ranges[2];

        return ranges;
    }

    public BeatmapPool getBeatmapsByYearRangeAndDifficulty(int startYear, int endYear, List<GameDifficulty> difficulties, int limit, String tag, List<GenreType> genres, List<LanguageType> languages) {
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
        double[][] difficultyRange = getDifficultyRanges(difficulties);

        BeatmapPool beatmapPool = new BeatmapPool();
        beatmapPool.setBeatmaps(beatmapRepository.findBeatmapsByApprovedDateRangeAndDifficulty(startDate, endDate,
                difficultyRange[0][0], difficultyRange[0][1],
                difficultyRange[1][0], difficultyRange[1][1],
                difficultyRange[2][0], difficultyRange[2][1],
                difficultyRange[3][0], difficultyRange[3][1],
                limit, "%" + tag + "%",
                genres.stream().map(GenreType::getValue).toList(),
                languages.stream().map(LanguageType::getValue).toList()));
        beatmapPool.setTotalBeatmapPoolSize(beatmapRepository.countBeatmapsByApprovedDateRangeAndDifficulty(startDate, endDate,
                difficultyRange[0][0], difficultyRange[0][1],
                difficultyRange[1][0], difficultyRange[1][1],
                difficultyRange[2][0], difficultyRange[2][1],
                difficultyRange[3][0], difficultyRange[3][1], "%" + tag + "%",
                genres.stream().map(GenreType::getValue).toList(),
                languages.stream().map(LanguageType::getValue).toList()));

        return beatmapPool;
    }

    public double[][] getDifficultyRanges(List<GameDifficulty> difficulties) {
        double[][] ranges = new double[4][2];

        // Define the difficulty ranges
        Map<GameDifficulty, double[]> difficultyRanges = new HashMap<>();
        difficultyRanges.put(GameDifficulty.EASY, new double[]{DifficultyCalc.DIFF_EASY_BAR, 1.0d});
        difficultyRanges.put(GameDifficulty.NORMAL, new double[]{DifficultyCalc.DIFF_NORMAL_BAR, DifficultyCalc.DIFF_EASY_BAR});
        difficultyRanges.put(GameDifficulty.HARD, new double[]{DifficultyCalc.DIFF_HARD_BAR, DifficultyCalc.DIFF_NORMAL_BAR});
        difficultyRanges.put(GameDifficulty.INSANE, new double[]{DifficultyCalc.DIFF_INSANE_BAR, DifficultyCalc.DIFF_HARD_BAR});

        // Fill the ranges array with the ranges for the specified difficulties
        for (int i = 0; i < difficulties.size(); i++) {
            ranges[i] = difficultyRanges.get(difficulties.get(i));
        }

        // Fill the remaining elements with [-1.0, -1.0] to indicate that they are not used
        for (int i = difficulties.size(); i < 4; i++) {
            ranges[i] = new double[]{-1.0, -1.0};
        }

        return ranges;
    }

    @Cacheable(value = "possibleAnswers")
    public List<String> getPossibleAnswers() {
        return beatmapRepository.findDistinctTitles();
    }

    @Cacheable(value = "possibleAnswers_artist")
    public List<String> getPossibleArtists() {
        return beatmapRepository.findDistinctArtists();
    }

    @Cacheable(value = "possibleAnswers_creator")
    public List<String> getPossibleCreators() {
        return beatmapRepository.findDistinctCreators();
    }

    public boolean addPlaycount(int beatmapsetId, int answer_playcount, int total_playcount) {
        Beatmap beatmap = beatmapRepository.findById(beatmapsetId).orElse(null);
        if (beatmap == null) {
            return false;
        }

        beatmap.setPlaycount_answer(beatmap.getPlaycount_answer() + answer_playcount);
        beatmap.setPlaycount(beatmap.getPlaycount() + total_playcount);

        beatmapRepository.save(beatmap);
        return true;
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

    @CacheEvict(value = {"beatmapCount", "possibleAnswers", "possibleAnswers_artist", "possibleAnswers_creator"}, allEntries = true)
    public Beatmap addBeatmap(String beatsetmap_id) throws IOException, ParseException {
        URL url = new URL("https://osu.ppy.sh/api/get_beatmaps?k=" + apiKey + "&s=" + beatsetmap_id);
        BufferedReader bf; String line = ""; String result="";
        bf = new BufferedReader(new InputStreamReader(url.openStream()));

        while((line=bf.readLine())!=null){
            result=result.concat(line);
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> jsonMap = mapper.readValue(result.substring(1, result.length() - 1), new TypeReference<Map<String, String>>() {
        });

        // Check if the beatmap already exists
        if (beatmapRepository.existsById(Integer.parseInt(jsonMap.get("beatmapset_id")))) {
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<String> tagList = getTags(jsonMap.get("artist"), jsonMap.get("title"), jsonMap.get("source"), jsonMap.get("tags"));

        // Convert the list of tags to a single string
        String tagString = String.join(" ", tagList);

        Beatmap beatmap = Beatmap.builder()
                .beatmapset_id(Integer.parseInt(jsonMap.get("beatmapset_id")))
                .artist(jsonMap.get("artist"))
                .title(jsonMap.get("title"))
                .creator(jsonMap.get("creator"))
                .language(jsonMap.get("language_id"))
                .genre(jsonMap.get("genre_id"))
                .tags(tagString)
                .approved_date(new Timestamp(dateFormat.parse(jsonMap.get("approved_date")).getTime()))
                .playcount(1)
                .playcount_answer(0)
                .blur(false)
                .build();

//        resourceService.getAudioAsync(Integer.parseInt(beatsetmap_id));
//        resourceService.getImageAsync(Integer.parseInt(beatsetmap_id), false);

        System.out.println("Adding beatmap: " + beatmap.getArtistAndTitle());

        return beatmapRepository.save(beatmap);
    }

    private Beatmap addBlur(String beatmapset_id) {
        Beatmap beatmap = beatmapRepository.findById(Integer.parseInt(beatmapset_id)).orElse(null);
        if (beatmap == null) {
            return null;
        }

        List<BeatmapReport> beatmapReport = beatmapReportRepository.findAllByBeatmapsetId(Integer.parseInt(beatmapset_id));
        beatmapReportRepository.deleteAll(beatmapReport);

        resourceService.getImage(Integer.parseInt(beatmapset_id), true);

        if (beatmap.isBlur()) return null;

        beatmap.setBlur(true);
        beatmap.setPlaycount(10);
        beatmap.setPlaycount_answer(5);

//        return null;

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
}
