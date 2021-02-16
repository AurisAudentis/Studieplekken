package blok2.controllers;

import blok2.daos.ICalendarPeriodForLockersDao;
import blok2.daos.ILocationDao;
import blok2.helpers.authorization.AuthorizedLocationController;
import blok2.model.calendar.CalendarPeriodForLockers;
import blok2.model.calendar.Period;
import blok2.model.reservables.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("locations/lockerCalendar")
public class CalendarPeriodsForLockersController extends AuthorizedLocationController {

    private final Logger logger = Logger.getLogger(CalendarPeriodsForLockersController.class.getSimpleName());

    private final ICalendarPeriodForLockersDao calendarPeriodForLockersDao;
    private final ILocationDao locationDao;

    @Autowired
    public CalendarPeriodsForLockersController(ICalendarPeriodForLockersDao calendarPeriodForLockersDao,
                                               ILocationDao locationDao) {
        this.calendarPeriodForLockersDao = calendarPeriodForLockersDao;
        this.locationDao = locationDao;
    }

    @GetMapping("/{locationId}")
    @PreAuthorize("permitAll()")
    public List<CalendarPeriodForLockers>
    getCalendarPeriodsForLockersOfLocation(@PathVariable("locationId") int locationId) {
        try {
            Location location = locationDao.getLocationById(locationId);
            return this.calendarPeriodForLockersDao.getCalendarPeriodsForLockersOfLocation(location.getName());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void addCalendarPeriodsForLockers(@RequestBody List<CalendarPeriodForLockers> calendarPeriodForLockers) {
        calendarPeriodForLockers.forEach(c -> isAuthorized(c.getLocation().getLocationId()));
        try {
            calendarPeriodForLockersDao.addCalendarPeriodsForLockers(calendarPeriodForLockers);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @PutMapping("/{locationId}")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void updateCalendarPeriodsForLockers(@PathVariable("locationId") int locationId,
                                                @RequestBody List<CalendarPeriodForLockers>[] fromAndTo) {
        isAuthorized(locationId);
        try {
            List<CalendarPeriodForLockers> from = fromAndTo[0];
            List<CalendarPeriodForLockers> to = fromAndTo[1];

            Location location = locationDao.getLocationById(locationId);

            // check for outdated view (perhaps some other user has changed the calendar periods in the meantime
            // between querying for the calendar periods for a location, and updating the calendar
            List<CalendarPeriodForLockers> currentView = calendarPeriodForLockersDao
                    .getCalendarPeriodsForLockersOfLocation(location.getName());

            // if the sizes dont match, the view must be different...
            if (from.size() != currentView.size()) {
                logger.log(Level.SEVERE, "updateCalendarPeriodsForLockers, conflict in frontends data view and actual data view");
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Wrong/Old view on data layer");
            }

            // if the sizes do match, check if the lists are equal before invoking
            // the 'equals' on a list, sort both lists based on 'starts at'
            currentView.sort(Comparator.comparing(Period::getStartsAt));
            from.sort(Comparator.comparing(Period::getStartsAt));

            if (!currentView.equals(from)) {
                logger.log(Level.SEVERE, "updateCalendarPeriodsForLockers, conflict in frontends data view and actual data view");
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Wrong/Old view on data layer");
            }

            // if the 'to' list is empty, all 'from' entries need to be deleted
            if (to.isEmpty()) {
                deleteCalendarPeriodsForLockers(from);
            }
            // if the 'from' list is empty, all 'to' entries need to be added
            else if (from.isEmpty()) {
                addCalendarPeriodsForLockers(to);
            } else {
                to.sort(Comparator.comparing(Period::getStartsAt));
                analyzeAndUpdateCalendarPeriodsForLockers(location.getName(), from, to);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    /**
     * Analyzing means:
     * - all periods to be updated need to be for the same location
     * - no periods in 'to' may overlap
     * - all periods need to end after the start
     * <p>
     * Prerequisites:
     * - The parameter 'to' needs to be sorted based on 'startsAt' as date
     * (not the comparison of the string 'startsAt', but compare the date)
     * - 'to' is not empty
     * <p>
     * Special remarks:
     * - If the analysis has been done and all requisites are met, updating
     * means: deleting 'from' and adding 'to'. The reason is that it is too
     * costly to search for differences between 'from' and 'to'. Therefore,
     * the strategy of deleting the 'from' periods and adding new 'to' periods
     * was chosen
     */
    private void
    analyzeAndUpdateCalendarPeriodsForLockers(String locationName,
                                              List<CalendarPeriodForLockers> from,
                                              List<CalendarPeriodForLockers> to) throws SQLException {
        // setup
        Location expectedLocation = locationDao.getLocationByName(locationName);

        // analyze the periods
        for (CalendarPeriodForLockers period : to) {
            // all locations must match the expected location
            if (!expectedLocation.equals(period.getLocation())) {
                logger.log(Level.SEVERE, "analyzeAndUpdateCalendarPeriodsForLockers, conflict in locations");
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Different locations in request");
            }

            // check if the end of all periods are after the start
            if (period.getEndsAt().isBefore(period.getStartsAt())) {
                logger.log(Level.SEVERE, "analyzeAndUpdateCalendarPeriodsForLockers, endsAt was before startsAt");
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "StartsAt must be before EndsAt");
            }
        }

        // delete all 'from' and add 'to'
        deleteCalendarPeriodsForLockers(from);
        addCalendarPeriodsForLockers(to);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void deleteCalendarPeriodsForLockers(@RequestBody List<CalendarPeriodForLockers> calendarPeriodForLockers) {
        calendarPeriodForLockers.forEach(cp -> isAuthorized(cp.getLocation().getLocationId()));
        try {
            calendarPeriodForLockersDao.deleteCalendarPeriodsForLockers(calendarPeriodForLockers);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }
}
