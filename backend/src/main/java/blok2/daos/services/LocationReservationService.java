package blok2.daos.services;

import blok2.daos.ILocationReservationDao;
import blok2.daos.db.DBLocationReservationDao;
import blok2.daos.repositories.LocationReservationRepository;
import blok2.daos.repositories.UserRepository;
import blok2.helpers.exceptions.NoSuchDatabaseObjectException;
import blok2.model.calendar.Timeslot;
import blok2.model.reservations.LocationReservation;
import blok2.model.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.threeten.extra.YearWeek;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationReservationService implements ILocationReservationDao {

    private final LocationReservationRepository locationReservationRepository;
    private final UserRepository userRepository;

    private final DBLocationReservationDao locationReservationDao;

    @Autowired
    public LocationReservationService(LocationReservationRepository locationReservationRepository,
                                      UserRepository userRepository,
                                      DBLocationReservationDao locationReservationDao) {
        this.locationReservationRepository = locationReservationRepository;
        this.userRepository = userRepository;
        this.locationReservationDao = locationReservationDao;
    }

    @Override
    public List<LocationReservation> getAllLocationReservationsOfUser(String userId) {
        return locationReservationRepository.findAllByUserId(userId);
    }

    @Override
    public LocationReservation getLocationReservation(String userId, Timeslot timeslot) {
        LocationReservation.LocationReservationId id = new LocationReservation.LocationReservationId(
                timeslot.getTimeslotSeqnr(), userId
        );

        return locationReservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchDatabaseObjectException(
                        String.format("No location reservation found with id '%s'", id)));
    }

    @Override
    public List<LocationReservation> getUnattendedLocationReservations(LocalDate date) {
        return locationReservationRepository.findAllUnattendedByDate(YearWeek.from(date).getYear(), YearWeek.from(date).getWeek(), DayOfWeek.from(date));
    }


    @Override
    public List<LocationReservation> getUnattendedLocationReservationsWith21PMRestriction(LocalDate date) {
        LocalDate dayBefore = date.minusDays(1);
        LocalDateTime yesterday21PM = LocalDateTime.of(dayBefore, LocalTime.of(21, 0)); // dateTime since previous mailing batch.
        LocalDateTime today21PM = LocalDateTime.of(date, LocalTime.of(21, 0)); // dateTime of this mailing batch.
        List<LocationReservation> locationReservations = locationReservationRepository.findAllUnattendedByDateAnd21PMRestriction(date.getYear(), YearWeek.from(date).getWeek(),
                date.getDayOfWeek(),  dayBefore.getYear(), YearWeek.from(dayBefore).getWeek(), dayBefore.getDayOfWeek(), yesterday21PM, today21PM);
        return locationReservations;
    }

    @Override
    public List<User> getUsersWithReservationForWindowOfTime(LocalDate start, LocalDate end) {

        return Collections.emptyList();
    }

    @Override
    public List<LocationReservation> getAllLocationReservationsOfTimeslot(Timeslot timeslot) {
        return locationReservationRepository.findAllByTimeslot(
                timeslot.getTimeslotSeqnr()
        );
    }

    @Override
    public long countReservedSeatsOfTimeslot(Timeslot timeslot) {
        return locationReservationRepository.countReservedSeatsOfTimeslot(
                timeslot.getTimeslotSeqnr());
    }

    @Override
    @javax.transaction.Transactional
    public void deleteLocationReservation(String userId, Timeslot timeslot) {
        locationReservationRepository.decrementCountByOne(timeslot.getTimeslotSeqnr());
        locationReservationRepository.deleteById(new LocationReservation.LocationReservationId(
                timeslot.getTimeslotSeqnr(), userId
        ));
    }

    @Override
    public LocationReservation addLocationReservation(LocationReservation locationReservation) {
        return locationReservationRepository.saveAndFlush(locationReservation);
    }

    @Override
    @Transactional
    public boolean setReservationAttendance(String userId, Timeslot timeslot, boolean attendance) {
        LocationReservation.LocationReservationId id = new LocationReservation.LocationReservationId(
                timeslot.getTimeslotSeqnr(), userId
        );

        LocationReservation locationReservation = locationReservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchDatabaseObjectException(
                        String.format("No location reservation found with id '%s'", id)));
        Boolean currentAttendance = locationReservation.getAttended();

        // if attendance goes from null or true to false, decrement reservation count
        if (!attendance && (currentAttendance == null || currentAttendance))
            locationReservation.getTimeslot().decrementAmountOfReservations();

        // if attendance goes from false to true, increment the current reservation count since this person is here now
        if (attendance && (currentAttendance != null && !currentAttendance))
            locationReservation.getTimeslot().incrementAmountOfReservations();

        locationReservation.setAttended(attendance);
        locationReservation = locationReservationRepository.saveAndFlush(locationReservation);

        return locationReservation.getAttended() == attendance;
    }

    @Override
    public boolean addLocationReservationIfStillRoomAtomically(LocationReservation reservation) throws SQLException {
        return locationReservationDao.addLocationReservationIfStillRoomAtomically(reservation);
    }

    @Override
    public void setNotScannedStudentsToUnattended(Timeslot timeslot) {
        List<LocationReservation> locationReservations = locationReservationRepository.findAllUnknownAttendanceByTimeslot(
                timeslot.getTimeslotSeqnr()
        );

        // for each unattended reservation, set the attendance to false and decrement the reservation count
        // so that other students are able to make use of the freed spot
        locationReservations.forEach((LocationReservation lr) -> {
            lr.setAttended(false);
            lr.getTimeslot().decrementAmountOfReservations();
        });

        locationReservationRepository.saveAll(locationReservations);
    }

    @Override
    public List<LocationReservation> getAllFutureLocationReservationsOfLocation(int locationId) {
        YearWeek n = YearWeek.now();
        return locationReservationRepository.findAllByLocationIdAndDateAfter(locationId, n.getYear(), n.getWeek(), LocalDate.now().getDayOfWeek());
    }

}
