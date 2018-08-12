package com.forcetower.sagres.database.model;

public class SagresCalendar {
    private String day;
    private String message;

    public SagresCalendar(String day, String message) {
        this.day = day;
        this.message = message;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SagresCalendar that = (SagresCalendar) o;

        if (!day.equals(that.day)) return false;
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + day.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }
}
