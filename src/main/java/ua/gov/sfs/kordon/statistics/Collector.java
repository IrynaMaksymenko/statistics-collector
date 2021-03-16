package ua.gov.sfs.kordon.statistics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class Collector {

    private static final String urlPattern = "http://kordon.sfs.gov.ua/uk/home/countries/%s/%s";
    private static final List<String> countries = Arrays.asList("pl", "sk", "ro", "hu", "md", "ru", "by");
    private static final List<String> directions = Arrays.asList("i", "o");

    public static void main(String[] args) {
        final File baseDir = new File("statistics-collector-output");
        createDirectory(baseDir.getAbsoluteFile());

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Running job");
            final long timer = System.currentTimeMillis();
            final File timestampDir = new File(baseDir.getAbsoluteFile(), new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss").format(new Date()));
            createDirectory(timestampDir.getAbsoluteFile());

            for (String country : countries) {
                final File countryDirectory = new File(timestampDir.getAbsoluteFile(), country);
                createDirectory(countryDirectory.getAbsoluteFile());
                for (String direction : directions) {
                    try {
                        URL url = new URL(format(urlPattern, country, direction));
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");
                        con.setConnectTimeout(5000);
                        con.setReadTimeout(5000);

                        int status = con.getResponseCode();
                        InputStream stream;

                        if (status > 299) {
                            stream = con.getErrorStream();
                        } else {
                            stream = con.getInputStream();
                        }

                        final File file = new File(countryDirectory.getAbsoluteFile(), format("%s.html", direction));
                        Files.copy(stream, file.toPath());
                        con.disconnect();
                    } catch (Exception e) {
                        System.out.printf("Failed to collect statistics for %s! %s%n", country, e);
                    }
                }
            }

            System.out.printf("Finished in %s ms%n", System.currentTimeMillis() - timer);
        }, 0, 3, TimeUnit.HOURS);
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

}
