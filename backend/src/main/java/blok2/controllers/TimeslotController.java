package blok2.controllers;

import blok2.daos.ITimeslotDao;
import blok2.helpers.authorization.AuthorizedLocationController;
import blok2.helpers.exceptions.NoSuchDatabaseObjectException;
import blok2.model.calendar.Timeslot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("locations/timeslots")
public class TimeslotController extends  AuthorizedLocationController {

    private final ITimeslotDao timeslotDAO;

    @Autowired
    public TimeslotController(ITimeslotDao calendarPeriodDao) {
        this.timeslotDAO = calendarPeriodDao;
    }

    @GetMapping("/details/{timeslotId}")
    @PreAuthorize("permitAll()")
    public Timeslot getTimeslot(@PathVariable("timeslotId") int timeslotId) {
        Timeslot timeslot = timeslotDAO.getTimeslot(timeslotId);
        if(timeslot == null)
            throw new NoSuchDatabaseObjectException("Timeslot does not exist");

        return timeslot;
    }

    @GetMapping("/{locationId}")
    @PreAuthorize("permitAll()")
    public List<Timeslot> getTimeslotsOfLocation(@PathVariable("locationId") int locationId) {
        return timeslotDAO.getTimeslotsOfLocation(locationId);
    }

    @PutMapping()
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void updateTimeslot(@Valid @RequestBody  Timeslot timeslot) {
        Timeslot original = timeslotDAO.getTimeslot(timeslot.getTimeslotSeqnr());
        // Validate original location
        isAuthorized(original.getLocationId());
        // Validate future location
        isAuthorized(timeslot.getLocationId());

        // Execute update
        timeslotDAO.updateTimeslot(timeslot);
    }


    @DeleteMapping
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void deleteTimeslot(@RequestBody Timeslot timeslot) {
        isAuthorized(timeslot.getLocationId());

        timeslotDAO.deleteTimeslot(timeslot);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public Timeslot addTimeslot(@RequestBody Timeslot timeslot) {
        isAuthorized(timeslot.getLocationId());

        return timeslotDAO.addTimeslot(timeslot);
    }

    @PutMapping("/{timeslotId}/repeatable")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public Timeslot setRepeatable(@PathVariable("timeslotId") Integer timeslotSequenceNumber, @RequestBody SetRepeatableBodyDao rep) {
        Timeslot ts = timeslotDAO.getTimeslot(timeslotSequenceNumber);
        isAuthorized(ts.getLocationId());
        ts.setRepeatable(rep.repeatable);
        return timeslotDAO.updateTimeslot(ts);
    }
}

class SetRepeatableBodyDao {
    public boolean repeatable;
}