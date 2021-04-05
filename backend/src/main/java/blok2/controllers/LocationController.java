package blok2.controllers;

import blok2.daos.IAccountDao;
import blok2.daos.ILocationDao;
import blok2.daos.ILocationTagDao;
import blok2.helpers.Pair;
import blok2.helpers.authorization.AuthorizedLocationController;
import blok2.helpers.LocationWithApproval;
import blok2.helpers.exceptions.AlreadyExistsException;
import blok2.mail.MailService;
import blok2.helpers.exceptions.NoSuchLocationException;
import blok2.helpers.exceptions.NoSuchUserException;
import blok2.model.reservables.Location;
import blok2.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This controller handles all requests related to locations.
 * Such as creating locations, list of locations, edit locations, ...
 */
@RestController
@RequestMapping("locations")
public class LocationController extends AuthorizedLocationController {

    private final Logger logger = LoggerFactory.getLogger(LocationController.class.getSimpleName());

    private final ILocationDao locationDao;
    private final ILocationTagDao locationTagDao;
    private final IAccountDao accountDao;

    private final MailService mailService;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    // *************************************
    // *   CRUD operations for LOCATIONS   *
    // *************************************

    @Autowired
    public LocationController(ILocationDao locationDao, ILocationTagDao locationTagDao, IAccountDao accountDao,
                              MailService mailService) {
        this.locationDao = locationDao;
        this.locationTagDao = locationTagDao;
        this.accountDao = accountDao;
        this.mailService = mailService;
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<Location> getAllLocations() {
        try {
            return locationDao.getAllLocations();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @GetMapping("/unapproved")
    public List<Location> getAllUnapprovedLocations() {
        try {
            return locationDao.getAllUnapprovedLocations();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @GetMapping("/{locationId}")
    @PreAuthorize("permitAll()")
    public Location getLocation(@PathVariable("locationId") int locationId) {
        try {
            return locationDao.getLocationById(locationId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @GetMapping("/nextReservableFroms")
    @PreAuthorize("permitAll()")
    public List<Pair<String, LocalDateTime>> getAllNextReservableFroms() {
        try {
            return locationDao.getAllLocationNextReservableFroms();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void addLocation(@RequestBody Location location) {
        isAuthorized((l,$) -> hasAuthority(l.getAuthority()), location);
        try {
            if (this.locationDao.getLocationByName(location.getName()) != null)
                throw new AlreadyExistsException("location name already in use");

            this.locationDao.addLocation(location);

            // This if statement is really important! Otherwise development mails could be sent to the admins...
            if (activeProfile.contains("prod")) {
                List<String> adminList = accountDao.getAdmins().stream().map(User::getMail).collect(Collectors.toList());
                String[] admins = new String[adminList.size()];
                adminList.toArray(admins);
                logger.info(String.format("Sending mail to admins to notify about creation of new location %s. Recipients are: %s", location.toString(), Arrays.toString(admins)));
                mailService.sendNewLocationMessage(admins, location);
            } else {
                logger.info(String.format("NOT Sending mail to admins to notify about creation of new location %s because the active profile does not contain 'prod'", location.toString()));
            }

            logger.info(String.format("New location %s added", location.getName()));
        } catch (SQLException | MessagingException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @PutMapping("/{locationId}")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void updateLocation(@PathVariable("locationId") int locationId, @RequestBody Location location) {
        isAuthorized(locationId);
        try {
            // Get the location that is currently in db
            Location cl = locationDao.getLocationById(locationId);

            // Make sure that only an admin could change the number of seats
            if (cl.getNumberOfSeats() != location.getNumberOfSeats() && !isAdmin())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Changing seats can only be done by admins");

            locationDao.updateLocation(locationId, location);
            logger.info(String.format("Location %d updated", locationId));
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @PutMapping("/{locationId}/approval")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void approveLocation(@PathVariable("locationId") int locationId, @RequestBody LocationWithApproval landa) {
        try {
            locationDao.approveLocation(landa.getLocation(), landa.isApproval());
            logger.info(String.format("Location %d updated", locationId));
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    //authority user
    //the updated location should be part of an authority the user is part of.
    @DeleteMapping("/{locationId}")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void deleteLocation(@PathVariable("locationId") int locationId) {
        isAuthorized(locationId);
        try {
            locationDao.deleteLocation(locationId);
            logger.info(String.format("Location %d deleted", locationId));
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @PostMapping("/{locationId}/volunteers/{userId}")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void addVolunteer(@PathVariable int locationId, @PathVariable String userId) {
        isAuthorized(locationId);
        try {
            if(locationDao.getLocationById(locationId) == null)
                throw new NoSuchLocationException("No such location");
            if(accountDao.getUserById(userId) == null)
                throw new NoSuchUserException("No such User");

            locationDao.addVolunteer(locationId, userId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @DeleteMapping("/{locationId}/volunteers/{userId}")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void deleteVolunteer(@PathVariable int locationId, @PathVariable String userId) {
        isAuthorized(locationId);
        try {
            if(locationDao.getLocationById(locationId) == null)
                throw new NoSuchLocationException("No such location");
            if(accountDao.getUserById(userId) == null)
                throw new NoSuchUserException("No such User");

            locationDao.deleteVolunteer(locationId, userId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @GetMapping("/{locationId}/volunteers")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public List<User> getVolunteers(@PathVariable int locationId) {
        isAuthorized(locationId);
        try {
            if(locationDao.getLocationById(locationId) == null)
                throw new NoSuchLocationException("No such location");

            return locationDao.getVolunteers(locationId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    // *****************************************
    // *   CRUD operations for LOCATION_TAGS   *
    // *****************************************

    /**
     * Following endpoint is a one-fits-all method: all tags that are supposed
     * to be set, must be provided in the body. Upon success only the tags
     * that have been provided, will be set for the location
     */
    @PutMapping("/tags/{locationId}")
    @PreAuthorize("hasAuthority('HAS_AUTHORITIES') or hasAuthority('ADMIN')")
    public void setupTagsForLocation(@PathVariable("locationId") int locationId,
                                     @RequestBody List<Integer> tagIds) {
        isAuthorized(locationId);
        try {
            logger.info(String.format("Setting up the tags for location '%s' with ids [%s]",
                    locationId, tagIds.stream().map(String::valueOf).collect(Collectors.joining(", "))));
            locationTagDao.deleteAllTagsFromLocation(locationId);
            locationTagDao.bulkAddTagsToLocation(locationId, tagIds);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    // **************************************************
    // *   Miscellaneous queries concerning locations   *
    // **************************************************

    /**
     * Returns an array of 7 strings for each location that is opened in the week specified by the given
     * week number in the given year.
     *
     * Each string is in the form of 'HH24:MI - HH24:MI' to indicate the opening and closing hour at
     * monday, tuesday, ..., sunday but can also be null to indicate that the location is not open that day.
     */
    @GetMapping("/overview/opening/{year}/{weekNr}")
    @PreAuthorize("permitAll()")
    public Map<String, String[]> getOpeningOverviewOfWeek(@PathVariable("year") int year,
                                                          @PathVariable("weekNr") int weekNr) {
        try {
            return locationDao.getOpeningOverviewOfWeek(year, weekNr);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

}
