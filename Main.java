import java.util.*;
import java.net.*;
import java.io.*;
import org.json.*;

public class Main {

    public static void main(String[] args) throws Exception {

        String regNo = "RA2311003030271";

        Set<String> seen = new HashSet<>();
        Map<String, Integer> scoreMap = new HashMap<>();

        for (int i = 0; i < 10; i++) {

            System.out.println("Fetching poll " + i);

            List<Event> events = fetchFromAPI(regNo, i);

            for (Event e : events) {

                String key = e.roundId + "_" + e.participant;

                if (!seen.contains(key)) {
                    seen.add(key);

                    int prev = scoreMap.getOrDefault(e.participant, 0);
                    scoreMap.put(e.participant, prev + e.score);

                } else {
                    System.out.println("Duplicate ignored: " + key);
                }
            }

            Thread.sleep(5000);
        }

        List<Map.Entry<String, Integer>> list =
                new ArrayList<>(scoreMap.entrySet());

        list.sort((a, b) -> b.getValue() - a.getValue());

        int total = 0;

        JSONArray leaderboard = new JSONArray();

        for (Map.Entry<String, Integer> entry : list) {

            JSONObject obj = new JSONObject();
            obj.put("participant", entry.getKey());
            obj.put("totalScore", entry.getValue());

            leaderboard.put(obj);

            total += entry.getValue();

            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        System.out.println("Total Score: " + total);

        submitResult(regNo, leaderboard);
    }

    static List<Event> fetchFromAPI(String regNo, int poll) {

    List<Event> list = new ArrayList<>();
    int attempts = 0;

    while (attempts < 3) {
        try {
            String urlStr = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages?regNo="
                    + regNo + "&poll=" + poll;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                throw new IOException("Server error: " + conn.getResponseCode());
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONObject obj = new JSONObject(response.toString());
            JSONArray events = obj.getJSONArray("events");

            for (int i = 0; i < events.length(); i++) {
                JSONObject e = events.getJSONObject(i);

                list.add(new Event(
                        e.getString("roundId"),
                        e.getString("participant"),
                        e.getInt("score")
                ));
            }

            return list;

        } catch (Exception e) {
            attempts++;
            System.out.println("Retrying poll " + poll + " attempt " + attempts);

            try { Thread.sleep(2000); } catch (Exception ex) {}
        }
    }

    return list;
}
    static void submitResult(String regNo, JSONArray leaderboard) {

    int attempts = 0;

    while (attempts < 5) {
        try {
            URL url = new URL("https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/submit");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject request = new JSONObject();
            request.put("regNo", regNo);
            request.put("leaderboard", leaderboard);

            OutputStream os = conn.getOutputStream();
            os.write(request.toString().getBytes());
            os.flush();

            int code = conn.getResponseCode();

            BufferedReader reader;

            if (code == 200) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);

                if (line.contains("isCorrect")) {
                    return; // success → exit
                }
            }

            throw new IOException("Retry needed");

        } catch (Exception e) {
            attempts++;
            System.out.println("Retrying submit " + attempts);

           try { Thread.sleep(6000); } catch (Exception ex) {}
        }
    }
}
}
class Event {
    String roundId;
    String participant;
    int score;

    Event(String r, String p, int s) {
        roundId = r;
        participant = p;
        score = s;
    }
}

