package ua.gov.sfs.kordon.statistics;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Collections.singletonList;

public class Collector {

    private static final String urlPattern = "http://kordon.sfs.gov.ua/uk/home/countries/%s/%s";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Running job");

            final LocalDateTime startTime = LocalDateTime.now();

            final File baseDir = createDirectory(null, "statistics-collector-output");

            for (Country country : Country.values()) {
                for (Direction direction : Direction.values()) {
                    collectStatistics(baseDir, startTime, country, direction);
                }
            }

            System.out.println("Finished job");
        }, 0, 3, TimeUnit.HOURS);
    }

    private static void collectStatistics(final File outputDirectory, final LocalDateTime startTime,
                                          final Country country, final Direction direction) {
        try {
            final File countryDirectory = createDirectory(outputDirectory.getAbsoluteFile(), country.getReadableName());
            final String url = format(urlPattern, country.name().toLowerCase(), direction.name().toLowerCase());
            final Document document = Jsoup.connect(url).timeout(5000).get();
            extractDataFromHtml(startTime, direction, countryDirectory, url, document);
        } catch (Exception ex) {
            System.out.printf("Failed to collect statistics for %s! %s%n", country, ex);
        }
    }

    private static void extractDataFromHtml(final LocalDateTime startTime, final Direction direction,
                                            final File countryDirectory, final String url, final Document document) {
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
                        return;
                    }
                    if (cells.size() < 3) {
                        System.out.printf("Unexpected html returned from %s: missing some td", url);
                        return;
                    }

                    final String checkpoint = getCellData(cells.get(0));
                    final String carWaitingTime = getCellData(cells.get(1));
                    final String cargoWaitingTime = getCellData(cells.get(2));

                    final File checkpointDirectory = createDirectory(countryDirectory.getAbsoluteFile(),
                            getCheckpointShortName(checkpoint));

                    writeStatistics(checkpointDirectory, direction, startTime, carWaitingTime, cargoWaitingTime);
                });
            } else {
                System.out.printf("Unexpected html returned from %s: missing tbody", url);
            }

        } else {
            System.out.printf("Unexpected html returned from %s: missing table with class 'responsive'", url);
        }
    }

    private static String getCellData(final Element cell) {
        if (!cell.select("img").isEmpty()) {
            return "-";
        }
        return cell.childNode(0).toString();
    }

    private static void writeStatistics(final File checkpointDirectory, final Direction direction,
                                        final LocalDateTime startTime,
                                        final String carWaitingTime, final String cargoWaitingTime) {
        try {
            final File file = new File(checkpointDirectory.getAbsoluteFile(),
                    format("%s.csv", direction.getReadableName()));
            if (file.createNewFile()) {
                Files.write(file.toPath(), singletonList(""
                        + "Дата,"
                        + "День тижня,"
                        + "Час очікування легкових автомобілів (годин:хвилин),"
                        + "Час очікування вантажних автомобілів (годин:хвилин)"));
            }

            Files.write(file.toPath(), singletonList(String.join(",",
                    startTime.format(dateTimeFormatter),
                    WeekDay.lookup(startTime.getDayOfWeek().getValue()).getReadableName(),
                    carWaitingTime,
                    cargoWaitingTime)),
                    APPEND);
        } catch (Exception e) {
            System.out.printf("Failed to save statistics for %s! %s%n", checkpointDirectory.getAbsolutePath(), e);
        }
    }

    private static File createDirectory(final File parentDirectory, final String name) {
        try {
            final File directory = new File(parentDirectory, name);
            if (!directory.exists()) {
                Files.createDirectory(directory.toPath());
            }
            return directory;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
