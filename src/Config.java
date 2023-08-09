import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.time.LocalTime;
import java.util.*;

public class Config {
    private static Map<String, String[]> myMap;

    public Config(String fileConfig) {
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(fileConfig)));
            StringBuilder jsonBuilder = new StringBuilder();
            String line = "";
            while (true) {
                line = bufferedReader.readLine();
                if (line == null)
                    break;
                jsonBuilder.append(line);
            }
            JSONObject jsonObject = new JSONObject(jsonBuilder.toString());
            myMap = new HashMap<>();
            for (String key : jsonObject.keySet()) {
                JSONArray jsonArray = jsonObject.getJSONArray(key);
                String[] values = new String[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    values[i] = jsonArray.getString(i);
                }
                myMap.put(key, values);
            }
            bufferedReader.close();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(-1);
        }
    }

    public boolean checkRequest(HttpRequest request) {
        // check whitelisting
        boolean check = false;
        String host = request.getHost();
        String[] listHost = myMap.get("whitelisting");
        try {
            for (int i = 0; listHost[i] != null; i++) {
                if (host.contains(listHost[i])) {
                    check = true;
                }
            }
        } catch (Exception e) {
        }
        // Check current time
        LocalTime curTime = LocalTime.now();
        TimeRange timeRange = getTimeAccess();
        if (timeRange.contain(curTime) == false)
            check = false;
        return check;
    }

    public static String[] getWhiteListing() {
        return myMap.get("whitelisting");
    }

    public int getCacheTime() {
        String[] parts = myMap.get("cache_time");
        String[] part = parts[0].split("#");
        int time = 0;
        if (part[1].equals("minutes") || part[1].equals("minute")) {
            time = time + Integer.parseInt(part[0]) * 60;
        } else if (part[1].equals("seconds") || part[1].equals("second")) {
            time = time + Integer.parseInt(part[0]);
        }
        return time;
    }

    public static TimeRange getTimeAccess() {
        String[] part1 = myMap.get("time");
        String[] parts = part1[0].split("-");
        String timeStart = parts[0];// 8:30
        String timeEnd = parts[1];// 23:30
        LocalTime startTime = LocalTime.of(Integer.parseInt(timeStart.split(":")[0]),
                Integer.parseInt(timeStart.split(":")[1]));
        LocalTime endTime = LocalTime.of(Integer.parseInt(timeEnd.split(":")[0]),
                Integer.parseInt(timeEnd.split(":")[1]));

        TimeRange timeRange = new TimeRange(startTime, endTime);
        return timeRange;
    }
}