package com.collectpro.backend.repository.projection;

import java.time.LocalDate;

public interface DailyCountProjection {
    LocalDate getDay();
    Long getCount();
}