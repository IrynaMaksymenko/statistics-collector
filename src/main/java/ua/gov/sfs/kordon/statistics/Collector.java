package ua.gov.sfs.kordon.statistics;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class Collector {

    private static final String urlPattern = "http://kordon.sfs.gov.ua/uk/home/countries/%s/%s";

    public static void main(String[] args) {
        final File baseDir = new File("statistics-collector-output");
        createDirectory(baseDir.getAbsoluteFile());

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Running job");

            final LocalDateTime startTime = LocalDateTime.now();

            for (Country country : Country.values()) {
                for (Direction direction : Direction.values()) {
                    collectStatistics(baseDir, startTime, country, direction);
                }
            }

            System.out.println("Finished job");
        }, 0, 3, TimeUnit.HOURS);
    }

    private static void collectStatistics(final File outputDirectory, final LocalDateTime startTime,
                                          final Country country,
                                          final Direction direction) {
        final File countryDirectory = new File(outputDirectory.getAbsoluteFile(), country.getReadableName());
        createDirectory(countryDirectory.getAbsoluteFile());

        try {
            final String url = format(urlPattern,
                    country.name().toLowerCase(), direction.name().toLowerCase());
            final Document document = Jsoup.connect(url).timeout(5000).get();

            final Optional<Element> table =
                    document.getElementsByClass("responsive").stream().findAny();
            if (table.isPresent()) {

                final Optional<Element> tableBody = table.get().select("tbody").stream().findAny();
                if (tableBody.isPresent()) {

                    final Elements rows = tableBody.get().select("tr");
                    if (rows.isEmpty()) {
                        System.out.printf("Unexpected html returned from %s: missing tr", url);
                    }

                    rows.forEach(row -> {
                        final List<Element> cells = new ArrayList<>(row.select("td"));
                        if (cells.isEmpty()) {
                            System.out.printf("Unexpected html returned from %s: missing td", url);
                        }
                        if (cells.size() < 3) {
                            System.out.printf("Unexpected html returned from %s: missing some td", url);
                        }

                        final String checkpoint = cells.get(0).childNode(0).toString();
                        final String carWaitingTime = cells.get(1).childNode(0).toString();
                        final String cargoWaitingTime = cells.get(2).childNode(0).toString();

                        final File checkpointDirectory = new File(countryDirectory.getAbsoluteFile(),
                                getCheckpointShortName(checkpoint));
                        createDirectory(checkpointDirectory);

                        writeStatistics(checkpointDirectory, direction,
                                startTime, carWaitingTime, cargoWaitingTime);
                    });
                } else {
                    System.out.printf("Unexpected html returned from %s: missing tbody", url);
                }

            } else {
                System.out.printf("Unexpected html returned from %s: missing table with class 'responsive'",
                        url);
            }
        } catch (IOException ex) {
            System.out.printf("Failed to collect statistics for %s! %s%n", country, ex);
        }
    }

    private static void writeStatistics(final File checkpointDirectory, final Direction direction,
                                        final LocalDateTime startTime,
                                        final String carWaitingTime, final String cargoWaitingTime) {
        try {
            final File file = new File(checkpointDirectory.getAbsoluteFile(),
                    format("%s.csv", direction.getReadableName()));
            if (file.createNewFile()) {
                Files.write(file.toPath(), Collections.singletonList(""
                        + "Дата,"
                        + "День тижня,"
                        + "Час очікування легкових автомобілів (годин:хвилин),"
                        + "Час очікування вантажних автомобілів (годин:хвилин)"));
            }

            Files.write(file.toPath(), Collections.singletonList(String.join(",",
                    startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    WeekDay.lookup(startTime.getDayOfWeek().getValue()).getReadableName(),
                    carWaitingTime,
                    cargoWaitingTime)),
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.out.printf("Failed to save statistics for %s! %s%n", checkpointDirectory.getAbsolutePath(), e);
        }
    }

    private static void createDirectory(final File directory) {
        try {
            if (!directory.exists()) {
                Files.createDirectory(directory.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCheckpointShortName(final String checkpointName) {
        if (checkpointName.startsWith("Пункт пропуску ")) {
            String shortName = checkpointName.substring(15);
            if (shortName.contains(".")) {
                shortName = shortName.substring(0, shortName.indexOf("."));
            } else if (shortName.contains(" митного поста")) {
                shortName = shortName.substring(0, shortName.indexOf(" митного поста"));
            }
            shortName = shortName.replaceAll("\"", "");
            shortName = shortName.replaceAll("„", "");
            shortName = shortName.replaceAll("”", "");
            return shortName;
        }
        System.out.printf("Could not shorten %s", checkpointName);
        return checkpointName;
    }

}
