package edu.cit.barcenas.queuems.feature.holidays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HolidayService {

    private final WebClient webClient;
    private final String countryCode;
    
    // Simple in-memory cache for holidays of the current year
    private final Map<LocalDate, HolidayResponse> holidayCache = new ConcurrentHashMap<>();
    private int cachedYear = -1;

    public HolidayService(WebClient.Builder webClientBuilder, @Value("${app.country-code:PH}") String countryCode) {
        this.webClient = webClientBuilder.baseUrl("https://date.nager.at/api/v3").build();
        this.countryCode = countryCode;
    }

    public boolean isPublicHoliday(LocalDate date) {
        return getHolidayStatus(date).isHoliday();
    }

    public HolidayStatusDTO getHolidayStatus(LocalDate date) {
        int year = date.getYear();
        
        // Refresh cache if it's a new year
        if (year != cachedYear) {
            refreshHolidayCache(year);
        }

        HolidayResponse holiday = holidayCache.get(date);
        if (holiday == null) {
            return new HolidayStatusDTO(date, false, null, null);
        }

        return new HolidayStatusDTO(date, true, holiday.name, holiday.localName);
    }

    private synchronized void refreshHolidayCache(int year) {
        if (year == cachedYear) return;

        try {
            List<HolidayResponse> response = webClient.get()
                    .uri("/PublicHolidays/{year}/{countryCode}", year, countryCode)
                    .retrieve()
                    .bodyToFlux(HolidayResponse.class)
                    .collectList()
                    .block();

            if (response != null) {
                holidayCache.clear();
                holidayCache.putAll(response.stream()
                        .collect(Collectors.toMap(
                                h -> LocalDate.parse(h.date),
                                h -> h,
                                (first, second) -> first)));
                cachedYear = year;
            }
        } catch (Exception e) {
            // Log error and fallback to allowing requests if API is down
            System.err.println("Error fetching holidays: " + e.getMessage());
        }
    }

    private static class HolidayResponse {
        public String date;
        public String localName;
        public String name;
        public String countryCode;
        public boolean fixed;
        public boolean global;
        public List<String> counties;
        public Integer launchYear;
        public List<String> types;
    }
}
