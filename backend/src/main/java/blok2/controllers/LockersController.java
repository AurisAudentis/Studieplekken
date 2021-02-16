package blok2.controllers;

import blok2.daos.ILocationDao;
import blok2.daos.ILockersDao;
import blok2.helpers.authorization.AuthorizedLocationController;
import blok2.model.reservables.Location;
import blok2.model.reservations.LockerReservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("lockers")
public class LockersController extends AuthorizedLocationController {

    private final Logger logger = Logger.getLogger(LockersController.class.getSimpleName());

    private final ILockersDao lockersDao;
    private final ILocationDao locationDao;

    @Autowired
    public LockersController(ILockersDao lockersDao, ILocationDao locationDao) {
        this.lockersDao = lockersDao;
        this.locationDao = locationDao;
    }

    @GetMapping("/status/{locationId}")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public List<LockerReservation> getLockerStatuses(@PathVariable("locationId") int locationId) {
        isAuthorized(locationId);
        try {
            Location location = locationDao.getLocationById(locationId);
            return lockersDao.getLockerStatusesOfLocation(location.getName());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

}
