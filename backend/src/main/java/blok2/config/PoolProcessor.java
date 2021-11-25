package blok2.config;

import blok2.daos.repositories.LocationReservationRepository;
import blok2.daos.repositories.TimeslotRepository;
import blok2.model.calendar.Timeslot;
import blok2.model.reservations.LocationReservation;
import blok2.scheduling.ReservationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class PoolProcessor implements Runnable{

    public static final int RANDOM_RESERVATION_DURATION_MINS = 10;

    private final ReservationManager reservationManager;
    private final LocationReservationRepository reservationRepository;
    private final TimeslotRepository timeslotRepository;


    @Autowired
    public PoolProcessor(ReservationManager reservationManager, LocationReservationRepository reservationRepository, TimeslotRepository timeslotRepository) {
        this.reservationManager = reservationManager;
        this.reservationRepository = reservationRepository;
        this.timeslotRepository = timeslotRepository;
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * Get a pool of reservations when available and process them.
     * Catch all exceptions, because this program must continue running
     * so that reservations can still be processed.
     */
    @Override
    public void run() {
        while (true) {
            try {
                List<LocationReservation> reservations = reservationManager.getPool();
                for (LocationReservation reservation : reservations) {
                    Optional<LocationReservation> optDbRes =  reservationRepository.findById(reservation.getId());
                    if (!optDbRes.isPresent()) {
                        continue; // Ignore.
                    }
                    reservation = optDbRes.get();
                    Timeslot dbTimeslot = timeslotRepository.getByTimeslotSeqnr(reservation.getTimeslot().getTimeslotSeqnr());
                    // TODO(ydndonck): Check if timeslot is reservable as well?
                    if (!LocalDateTime.now().isAfter(dbTimeslot.getReservableFrom().plusMinutes(RANDOM_RESERVATION_DURATION_MINS))) {
                        rejectReservation(reservation);
                        continue;
                    }
                    if (dbTimeslot.getAmountOfReservations() + 1 > dbTimeslot.getSeatCount()) {
                        rejectReservation(reservation);
                        continue;
                    }
                    approveReservation(dbTimeslot, reservation);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Transactional
    public void approveReservation(Timeslot timeslot, LocationReservation reservation) {
        timeslot.incrementAmountOfReservations();
        timeslotRepository.save(timeslot);
        reservation.setState(LocationReservation.State.APPROVED);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void rejectReservation(LocationReservation reservation) {
        reservation.setState(LocationReservation.State.REJECTED);
        reservationRepository.save(reservation);
    }

}
