package com.ass1.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class DatabaseConnector {

    private static final String URL = "jdbc:sqlite:database.db";
    private static final Path DATASET_PATH = Path.of("solution/src/com/ass1/Database/dataset.csv");
    private static final String DATASET_RESOURCE_PATH = "/com/ass1/Database/dataset.csv";

    /**
     * Get a connection to the SQLite database.
     */
    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Fills the database if it is empty or skips filling if a
     * database is found and the cities table already exists.
     */
    public static void initializeDatabase() throws SQLException {
        System.out.println("Initializing database...");

        String tableName = "cities";
        if (databaseInitialized()) {
            System.out.println("Database already exists. Skipping initialization.");
            return;
        } else {
            System.out.println("Database does not exist. Creating and populating the database.");
        }

        String createSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    geoname_id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    country_code TEXT NOT NULL,
                    country_name TEXT NOT NULL,
                    population INTEGER NOT NULL,
                    timezone TEXT,
                    coordinates TEXT
                )
                """.formatted(tableName);

        String insertSql = """
                INSERT INTO cities (
                    geoname_id, name, country_code, country_name, population, timezone, coordinates
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create cities table", e);
        }

        try (Connection conn = connect(); BufferedReader reader = openDatasetReader()) {

            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                String line = reader.readLine(); // skip header
                System.out.println("Reading dataset and inserting into database...");
                System.out.println("Header: " + line);
                while ((line = reader.readLine()) != null) {
                    System.out.println("Processing line: " + line);
                    String[] parts = line.split(";", -1);
                    if (parts.length < 7) {
                        continue;
                    }

                    ps.setInt(1, Integer.parseInt(parts[0].trim()));
                    ps.setString(2, parts[1]);
                    ps.setString(3, parts[2]);
                    ps.setString(4, parts[3]);
                    ps.setInt(5, Integer.parseInt(parts[4].trim()));
                    ps.setString(6, parts[5]);
                    ps.setString(7, parts[6]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();

            System.out.println("Database initialized with fresh dataset.");
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to initialize cities dataset", e);
        }
    }

    /**
     * Checks if the database is initialized by checking if the cities table contains at least
     * one row of data.
     *
     * @return true if the database is initialized, false otherwise.
     */
    private static boolean databaseInitialized() {
        String sql = "SELECT COUNT(*) AS count FROM cities";

        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt("count");
                return count > 0;
            }
        } catch (SQLException e) {
            // If the table doesn't exist, we assume the database is not initialized
            return false;
        }
        return false;
    }

    /**
     * Opens a BufferedReader for the dataset CSV file. It first attempts to load the dataset
     * from the classpath, and if that fails, it tries to load it from the file system.
     *
     * @return A BufferedReader for the dataset CSV file.
     * @throws IOException If the dataset cannot be found in either location.
     */
    private static BufferedReader openDatasetReader() throws IOException {
        InputStream resource = DatabaseConnector.class.getResourceAsStream(DATASET_RESOURCE_PATH);
        if (resource != null) {
            return new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
        }
        if (Files.exists(DATASET_PATH)) {
            return Files.newBufferedReader(DATASET_PATH);
        }
        throw new IOException("Dataset not found at classpath '" + DATASET_RESOURCE_PATH + "' or file path '" + DATASET_PATH + "'");
    }

    public static void printAllData() throws SQLException {
        String sql = """
                SELECT * FROM cities
                """;

        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("geoname_id | name | country_code | country_name | population | timezone | coordinates");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | %d | %s | %s%n",
                        rs.getInt("geoname_id"),
                        rs.getString("name"),
                        rs.getString("country_code"),
                        rs.getString("country_name"),
                        rs.getInt("population"),
                        rs.getString("timezone"),
                        rs.getString("coordinates"));
            }
        }
    }

    /**
     * Returns the total population of a given country.
     *
     * @param countryName The name of the country.
     * @return The total population of the country, or 0 if the country is not found.
     */
    public Integer getPopulationofCountry(String countryName) {
        String sql = """
                SELECT COALESCE(SUM(population), 0) AS total_population
                FROM cities
                WHERE country_name = ?
                """;

        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, countryName);
            try (ResultSet result = stmt.executeQuery()) {
                return result.getInt("total_population");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get country population", e);
        }
    }

    /**
     * Returns the number of cities in a given country that meet a specified population
     * threshold and comparison operator.
     * @param countryName Name of the country.
     * @param threshold Population threshold.
     * @param comp Comparison selector: "min" means at least the threshold, "max" means
     *             at most the threshold.
     * @return The number of cities that meet the criteria.
     */
    public Integer getNumberofCities(String countryName, Integer threshold, String comp) {
        String op = resolveComparisonOperator(comp);
        String sql = """
                SELECT COUNT(*) AS city_count
                FROM cities
                WHERE country_name = ?
                  AND population %s ?
                """.formatted(op);

        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, countryName);
            stmt.setInt(2, threshold);
            try (ResultSet result = stmt.executeQuery()) {
                return result.getInt("city_count");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get number of cities", e);
        }
    }

    /**
     * Returns the number of countries that have at least the specified number of cities,
     * where every counted city meets the population threshold and comparison operator.
     * @param citycount Number of cities in the country.
     * @param threshold Population threshold for each city.
     * @param comp Comparison selector: "min" means at least the threshold, "max" means
     *             at most the threshold.
     * @return The number of countries that meet the criteria.
     */
    public Integer getNumberofCountries(Integer citycount, Integer threshold, String comp) {
        String op = resolveComparisonOperator(comp);
        String sql = """
                SELECT COUNT(*) AS country_count
                FROM (
                    SELECT country_name
                    FROM cities
                    WHERE population %s ?
                    GROUP BY country_name
                    HAVING COUNT(*) >= ?
                ) filtered
                """.formatted(op);

        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, threshold);
            stmt.setInt(2, citycount);
            try (ResultSet result = stmt.executeQuery()) {
                return result.getInt("country_count");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get number of countries", e);
        }
    }

    /**
     * Returns the number of countries that have at least the specified number of cities,
     * where every counted city has a population within the specified range (inclusive).
     * @param citycount Number of cities in the country.
     * @param minpopulation Minimum population of each city.
     * @param maxpopulation Maximum population of each city.
     * @return The number of countries that meet the criteria.
     */
    public Integer getNumberofCountriesMM(Integer citycount, Integer minpopulation, Integer maxpopulation) {
        String sql = """
                SELECT COUNT(*) AS country_count
                FROM (
                    SELECT country_name
                    FROM cities
                    WHERE population BETWEEN ? AND ?
                    GROUP BY country_name
                    HAVING COUNT(*) >= ?
                ) filtered
                """;

        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, minpopulation);
            stmt.setInt(2, maxpopulation);
            stmt.setInt(3, citycount);
            try (ResultSet result = stmt.executeQuery()) {
                return result.getInt("country_count");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get number of countries in population range", e);
        }
    }

    /**
     * Resolves a requirement comparison selector to its SQL equivalent.
     * @param comp "min" for at least, or "max" for at most.
     * @return The SQL comparison operator.
     */
    private String resolveComparisonOperator(String comp) {
        if (comp == null) {
            throw new IllegalArgumentException("Comparison operator cannot be null");
        }
        return switch (comp.trim()) {
            case "min" -> ">=";
            case "max" -> "<=";
            default -> throw new IllegalArgumentException("Unsupported comparison operator: " + comp);
        };
    }
}
