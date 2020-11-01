package blok2.model.calendar;

import blok2.model.reservables.Location;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import static java.time.temporal.ChronoUnit.SECONDS;

public class CalendarPeriod extends Period implements Cloneable {
    private int id;
    private Location location;
    private LocalTime openingTime; // time: hh:mm
    private LocalTime closingTime; // time: hh:mm
    private LocalDateTime reservableFrom; // datetime: YYYY-MM-DD hh:mm
    private boolean reservable;
    private int reservableTimeslotSize;

    private List<Timeslot> timeslots = Collections.emptyList();

    public CalendarPeriod() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CalendarPeriod that = (CalendarPeriod) o;
        return reservable == that.reservable &&
                reservableTimeslotSize == that.reservableTimeslotSize &&
                location.equals(that.location) &&
                openingTime.equals(that.openingTime) &&
                closingTime.equals(that.closingTime) &&
                reservableFrom.equals(that.reservableFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), location, openingTime, closingTime, reservableFrom, reservable, reservableTimeslotSize);
    }


    @Override
    public String toString() {
        return "CalendarPeriod{" +
                "location=" + location +
                ", startsAt='" + getStartsAt() + '\'' +
                ", endsAt='" + getEndsAt() + '\'' +
                ", openingTime='" + openingTime + '\'' +
                ", closingTime='" + closingTime + '\'' +
                ", reservableFrom='" + reservableFrom + '\'' +
                ", reservable='" + reservable + '\'' +
                ", reservableTimeslotSize='" + reservableTimeslotSize + '\'' +
                '}';
    }

    @Override
    public CalendarPeriod clone() {
        try {
            CalendarPeriod clone = (CalendarPeriod) super.clone();
            clone.location = location.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
    }

    public LocalDateTime getReservableFrom() {
        return reservableFrom;
    }

    public void setReservableFrom(LocalDateTime reservableFrom) {
        this.reservableFrom = reservableFrom;
    }


    public boolean isReservable() {
        return reservable;
    }

    public void setReservable(boolean reservable) {
        this.reservable = reservable;
    }

    public int getReservableTimeslotSize() {
        return reservableTimeslotSize;
    }

    public void setReservableTimeslotSize(int reservableTimeslotSize) {
        this.reservableTimeslotSize = reservableTimeslotSize;
    }

    public List<Timeslot> getTimeslots() {
        return timeslots;
    }

    public void setTimeslots(List<Timeslot> timeslots) {
        this.timeslots = timeslots;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    /**
     * The length of time the location is open (in seconds)
     * @return
     */
    public int getOpenHoursDuration() {
        return Math.toIntExact(SECONDS.between(getOpeningTime(), getClosingTime()));
    }
}
