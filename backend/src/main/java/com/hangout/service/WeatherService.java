package com.hangout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hangout.dto.WeatherDto;
import com.hangout.model.Event;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final int MAX_FORECAST_DAYS = 16;
    private static final long CACHE_MINUTES = 60;

    // Position de repli si l'adresse n'est pas reconnue (Bizerte).
    // Mets ces deux lignes en commentaire + remplace l'appel dans getWeatherForEvent
    // si tu préfères afficher "introuvable" plutôt qu'une météo par défaut.
    private static final double DEFAULT_LAT = 37.2744;
    private static final double DEFAULT_LON = 9.8739;

    private final RestaurantRepository restaurantRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private record CacheEntry(WeatherDto data, LocalDateTime at) {}
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, double[]> geoCache = new ConcurrentHashMap<>();

    public WeatherDto getWeatherForEvent(Event event) {
        LocalDateTime when = event.getEventDate();
        long daysAhead = ChronoUnit.DAYS.between(LocalDate.now(), when.toLocalDate());

        if (daysAhead > MAX_FORECAST_DAYS) {
            return unavailable("Les prévisions sont disponibles à 16 jours maximum.");
        }
        if (daysAhead < 0) {
            return unavailable("Événement déjà passé.");
        }

        Restaurant r = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
        double[] coords = (r == null) ? null : geocode(r.getAddress());
        if (coords == null) {
            coords = new double[]{DEFAULT_LAT, DEFAULT_LON};
        }
        double lat = coords[0];
        double lon = coords[1];

        String key = lat + "|" + lon + "|" + when.toLocalDate() + "|" + when.getHour();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.at().isAfter(LocalDateTime.now().minusMinutes(CACHE_MINUTES))) {
            return cached.data();
        }

        try {
            String date = when.toLocalDate().toString();
            String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m"
                + "&daily=temperature_2m_max,temperature_2m_min"
                + "&start_date=" + date + "&end_date=" + date
                + "&timezone=auto";

            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            if (root == null || !root.has("hourly")) {
                return unavailable("Réponse météo invalide.");
            }

            int h = when.getHour();
            JsonNode hourly = root.get("hourly");
            JsonNode daily = root.get("daily");

            int code = hourly.get("weather_code").get(h).asInt();
            WeatherDto dto = WeatherDto.builder()
                .available(true)
                .date(date)
                .temperature(hourly.get("temperature_2m").get(h).asDouble())
                .tempMin(daily.get("temperature_2m_min").get(0).asDouble())
                .tempMax(daily.get("temperature_2m_max").get(0).asDouble())
                .precipitationProbability(hourly.get("precipitation_probability").get(h).asInt())
                .windSpeed(hourly.get("wind_speed_10m").get(h).asDouble())
                .weatherCode(code)
                .condition(describe(code))
                .icon(iconFor(code))
                .build();

            cache.put(key, new CacheEntry(dto, LocalDateTime.now()));
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return unavailable("Service météo momentanément indisponible.");
        }
    }

    /** Cherche la ville dans l'adresse : essaie chaque morceau, en partant de la fin. */
    private double[] geocode(String address) {
        if (address == null || address.isBlank()) return null;

        String[] parts = address.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String name = parts[i].trim();
            // ignore les morceaux vides, trop courts ou purement numériques (code postal)
            if (name.length() < 3 || name.matches("[\\d\\s-]+")) continue;

            double[] cached = geoCache.get(name);
            if (cached != null) return cached;

            try {
                String url = "https://geocoding-api.open-meteo.com/v1/search?count=1&name="
                    + URLEncoder.encode(name, StandardCharsets.UTF_8);
                JsonNode root = restTemplate.getForObject(url, JsonNode.class);
                if (root != null && root.has("results") && root.get("results").size() > 0) {
                    JsonNode f = root.get("results").get(0);
                    double[] c = {f.get("latitude").asDouble(), f.get("longitude").asDouble()};
                    geoCache.put(name, c);
                    return c;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private WeatherDto unavailable(String msg) {
        return WeatherDto.builder().available(false).message(msg).build();
    }

    private String describe(int c) {
        if (c == 0) return "Ciel dégagé";
        if (c <= 2) return "Peu nuageux";
        if (c == 3) return "Couvert";
        if (c == 45 || c == 48) return "Brouillard";
        if (c >= 51 && c <= 57) return "Bruine";
        if (c >= 61 && c <= 67) return "Pluie";
        if (c >= 71 && c <= 77) return "Neige";
        if (c >= 80 && c <= 82) return "Averses";
        if (c >= 95) return "Orage";
        return "Variable";
    }

    private String iconFor(int c) {
        if (c == 0) return "sunny";
        if (c <= 3) return "cloudy";
        if (c == 45 || c == 48) return "fog";
        if (c >= 51 && c <= 67) return "rain";
        if (c >= 71 && c <= 77) return "snow";
        if (c >= 80 && c <= 82) return "rain";
        if (c >= 95) return "storm";
        return "cloudy";
    }
}