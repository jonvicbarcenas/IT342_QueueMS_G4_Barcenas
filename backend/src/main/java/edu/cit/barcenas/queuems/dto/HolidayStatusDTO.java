package edu.cit.barcenas.queuems.dto;

import java.time.LocalDate;

public class HolidayStatusDTO {
    private LocalDate date;
    private boolean holiday;
    private String name;
    private String localName;

    public HolidayStatusDTO() {
    }

    public HolidayStatusDTO(LocalDate date, boolean holiday, String name, String localName) {
        this.date = date;
        this.holiday = holiday;
        this.name = name;
        this.localName = localName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isHoliday() {
        return holiday;
    }

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }
}
