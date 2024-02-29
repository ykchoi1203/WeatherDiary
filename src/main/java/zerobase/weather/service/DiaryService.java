package zerobase.weather.service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DiaryService {
    private final DiaryRepository diaryRepository;
    private final DateWeatherRepository dateWeatherRepository;

    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);


    @Value("${openweathermap.key}")
    private String apiKey;

    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * *")    // 매일 01시에 실행
    public void saveWeatherDate() {
        logger.info("started to save weather date");
        dateWeatherRepository.save(getWeatherFromApi());
        logger.info("ended to save weather date");
    }

    @Transactional
    public void createDiary(LocalDate date, String text) {
        logger.info("started to create diary");
        // 날씨 데이터 가져오기 (API에서 가져오기 또는 DB에서 가져오기)
        DateWeather dateWeather = getDateWeather(date);


        Diary diary = new Diary();
        diary.setDateWeather(dateWeather);
        diary.setText(text);

        diaryRepository.save(diary);
        logger.info("ended to create diary");

    }

    public List<Diary> readDiary(LocalDate date) {
        logger.debug("started to read diary");
        if(date.isAfter(LocalDate.ofYearDay(3050, 1))) {
            logger.error("readDiary - 너무 미래의 날짜입니다.");
            throw new InvalidDate();
        }
        return diaryRepository.findAllByDate(date);
    }

    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        logger.debug("started to read diaries");
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    @Transactional
    public void updateDiary(LocalDate date, String text) {
        logger.info("started to update diary");
        Diary diary = diaryRepository.getFirstByDate(date);

        diary.setText(text);

        diaryRepository.save(diary);
        logger.info("ended to update diary");
    }

    @Transactional
    public void deleteDiary(LocalDate date) {
        logger.info("started to delete diary");
        diaryRepository.deleteAllByDate(date);
        logger.info("ended to delete diary");
    }

    private String getWeatherString(String cityName) {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&appid=" + apiKey;
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader br;
            int responseCode = connection.getResponseCode();
            if(responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            String inputLine;
            StringBuilder response = new StringBuilder();
            while((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }

            br.close();

            return response.toString();
        } catch (Exception e) {
           return "failed to get response";
        }
    }

    private Map<String, Object> parseWeather(String jsonString) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;

        try {
            jsonObject = (JSONObject) jsonParser.parse(jsonString);
            Map<String, Object> resultMap = new HashMap<>();

            JSONObject mainData = (JSONObject) jsonObject.get("main");
            resultMap.put("temp", mainData.get("temp"));


            JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
            JSONObject weatherData = (JSONObject) weatherArray.get(0);
            resultMap.put("main", weatherData.get("main"));
            resultMap.put("icon", weatherData.get("icon"));

            return resultMap;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private DateWeather getWeatherFromApi() {
        String weatherData = getWeatherString("seoul");

        // 파싱된 데이터 + 우리 db에 저장하기
        Map<String, Object> parsedWeather = parseWeather(weatherData);

        DateWeather dateWeather = new DateWeather();

        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parsedWeather.get("main").toString());
        dateWeather.setIcon(parsedWeather.get("icon").toString());
        dateWeather.setTemperature((double)parsedWeather.get("temp"));

        return dateWeather;
    }

    private DateWeather getDateWeather(LocalDate date) {
        List<DateWeather> list = dateWeatherRepository.findAllByDate(date);
        if(list.size() == 0) {
            // 새로 api에서 날씨 정보를 가져와야함.
            return getWeatherFromApi();
        } else {
            return list.get(0);
        }
    }

}
