package blok2.daos;

import blok2.model.calendar.Timeslot;
import blok2.model.reservations.LocationReservation;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface ILocationReservationDao extends IDao {

    List<LocationReservation> getAllLocationReservationsOfUser(String augentID) throws SQLException;

    LocationReservation getLocationReservation(String augentID, Timeslot timeslot) throws SQLException;

    void deleteLocationReservation(String augentID, Timeslot timeslot) throws SQLException;

    void addLocationReservation(LocationReservation locationReservation) throws SQLException;

    LocationReservation scanStudent(String location, String barcode) throws SQLException;

    void setAllStudentsOfLocationToAttended(String location, LocalDate date) throws SQLException;

    long countReservedSeatsOfTimeslot(Timeslot timeslot) throws SQLException;

    List<LocationReservation> getAbsentStudents(String location, LocalDate date) throws SQLException;

    List<LocationReservation> getPresentStudents(String location, LocalDate date) throws SQLException;

    void setReservationToUnAttended(String augentId, LocalDate date) throws SQLException;

    List<LocationReservation> getAllLocationReservationsOfTimeslot(Timeslot timeslot) throws SQLException;

     boolean addLocationReservationIfStillRoomAtomically(LocationReservation reservation) throws SQLException;
}
