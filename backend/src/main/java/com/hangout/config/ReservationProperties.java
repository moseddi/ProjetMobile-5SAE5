package com.hangout.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Business rules used to compute availability (see application.properties, prefix "reservation").
 */
@Data
@Component
@ConfigurationProperties(prefix = "reservation")
public class ReservationProperties {
    /** How long a table is held for one reservation. */
    private int slotDurationMinutes = 120;
    /** Seats used when a restaurant has no seatingCapacity set. */
    private int defaultCapacity = 40;
    /** Last reservation accepted this many minutes before closing time. */
    private int lastSeatingMinutesBeforeClose = 60;
    private int minPartySize = 1;
    private int maxPartySize = 20;
}
