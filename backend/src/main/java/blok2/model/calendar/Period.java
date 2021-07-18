package blok2.model.calendar;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.time.LocalDate;
import java.util.Objects;

@MappedSuperclass
public class Period {

    @Column(name = "starts_at")
    private LocalDate startsAt;

    @Column(name = "ends_at")
    private LocalDate endsAt;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Period period = (Period) o;
        return Objects.equals(startsAt, period.startsAt) &&
                Objects.equals(endsAt, period.endsAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startsAt, endsAt);
    }

    public LocalDate getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDate startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDate getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDate endsAt) {
        this.endsAt = endsAt;
    }
}
