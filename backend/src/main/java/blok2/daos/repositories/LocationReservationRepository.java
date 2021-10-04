package blok2.daos.repositories;

import blok2.model.reservations.LocationReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface LocationReservationRepository extends JpaRepository<LocationReservation, LocationReservation.LocationReservationId> {

    @Query("select lr from LocationReservation lr where lr.id.userId = ?1")
    List<LocationReservation> findAllByUserId(String userId);

    @Query("select lr from LocationReservation lr where lr.id.timeslotSequenceNumber = ?1")
    List<LocationReservation> findAllByTimeslot(int sequenceNumber);

    @Query("select lr from LocationReservation lr where lr.timeslot.timeslotDate = ?1 and lr.attended = false")
    List<LocationReservation> findAllUnattendedByDate(LocalDate date);

    @Query("select lr from LocationReservation lr where (lr.timeslot.timeslotDate = ?1 or lr.timeslot.timeslotDate = ?2) and lr.updatedAt >= ?3 and lr.updatedAt < ?4 and lr.attended = false")
    List<LocationReservation> findAllUnattendedByDateAnd21PMRestriction(LocalDate date1, LocalDate date2, LocalDateTime yesterday21PM, LocalDateTime today21PM);

    @Query("select lr from LocationReservation lr where lr.id.timeslotSequenceNumber = ?1 and lr.attended is null")
    List<LocationReservation> findAllUnknownAttendanceByTimeslot(int sequenceNumber);

    @Query("select count(lr) from LocationReservation lr where lr.id.timeslotSequenceNumber = ?1")
    int countReservedSeatsOfTimeslot(int sequenceNumber);

    @Query("select lr from LocationReservation lr " +
            "   join Timeslot t on lr.id.timeslotSequenceNumber = t.timeslotSequenceNumber " +
            "where t.locationId = ?1 " +
            "and t.timeslotDate > ?2 ")
    List<LocationReservation> findAllByLocationIdAndDateAfter(int locationId, LocalDate date);


    /**
     * Decrements the timeslot reservation count by one on delete.
     *
     * This is a named query (cfr. orm.xml)
     * @param timeslotId id of the to decrement timeslot
     */
    @Modifying
    void decrementCountByOne(int timeslotId);
}
