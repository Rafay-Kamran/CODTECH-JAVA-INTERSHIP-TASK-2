 import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class RestApiClient {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.print("Enter city name: ");
            String city = scanner.nextLine().trim();

            // Step 1: Get coordinates
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + city;
            String geoResponse = getHttpResponse(geoUrl);

            double latitude = extractDouble(geoResponse, "\"latitude\":");
            double longitude = extractDouble(geoResponse, "\"longitude\":");
            String country = extractString(geoResponse, "\"country\":\"");

            if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                System.out.println("City not found. Check spelling.");
                return;
            }

            // Step 2: Fetch weather
            String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                                "&longitude=" + longitude + "&current_weather=true";

            String weatherResponse = getHttpResponse(weatherUrl);

            // Extract "current_weather" block
            String currentWeatherJson = extractBlock(weatherResponse, "\"current_weather\":{", "}");

            double temperature = extractDouble(currentWeatherJson, "\"temperature\":");
            double windspeed = extractDouble(currentWeatherJson, "\"windspeed\":");
            int weathercode = (int) extractDouble(currentWeatherJson, "\"weathercode\":");
            String time = extractString(currentWeatherJson, "\"time\":\"");

            // Display
            System.out.println("\n===== Weather Report =====");
            System.out.println("Location: " + city + ", " + country);
            System.out.println("Latitude: " + latitude + " | Longitude: " + longitude);

            System.out.println("Temperature: " + temperature + " °C");
            System.out.println("Wind Speed: " + windspeed + " km/h");
            System.out.println("Weather Code: " + weathercode);
            System.out.println("Time: " + time);

            System.out.println("==========================");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    // HTTP GET
    private static String getHttpResponse(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();
        conn.disconnect();
        return response.toString();
    }

    // Extract numeric
    private static double extractDouble(String json, String key) {
        try {
            int index = json.indexOf(key);
            if (index == -1) return Double.NaN;
            index += key.length();
            StringBuilder num = new StringBuilder();
            while (index < json.length() &&
                   (Character.isDigit(json.charAt(index)) || json.charAt(index) == '.' || json.charAt(index) == '-')) {
                num.append(json.charAt(index++));
            }
            return Double.parseDouble(num.toString());
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    // Extract string
    private static String extractString(String json, String key) {
        try {
            int index = json.indexOf(key);
            if (index == -1) return "Unknown";
            index += key.length();
            StringBuilder str = new StringBuilder();
            while (index < json.length() && json.charAt(index) != '"') {
                str.append(json.charAt(index++));
            }
            return str.toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Extract nested block like {...}
    private static String extractBlock(String json, String startKey, String endChar) {
        int start = json.indexOf(startKey);
        if (start == -1) return "";
        start += startKey.length();
        int braceCount = 1;
        int i = start;
        while (i < json.length() && braceCount > 0) {
            if (json.charAt(i) == '{') braceCount++;
            else if (json.charAt(i) == '}') braceCount--;
            i++;
        }
        return json.substring(start, i - 1);
    }
}
