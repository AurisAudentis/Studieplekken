package blok2.controllers;

import blok2.daos.IBuildingDao;
import blok2.model.Building;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("api/building")
public class BuildingController {
    private final Logger logger = LoggerFactory.getLogger(AuthorityController.class.getSimpleName());
    private final IBuildingDao buildingDao;

    @Autowired
    public BuildingController(IBuildingDao buildingDao) {
        this.buildingDao = buildingDao;
    }

    // *************************************
    // *   CRUD operations for Building   *
    // *************************************/

    @GetMapping
    public List<Building> getAllAuthorities() {
        try {
            return buildingDao.getAllBuildings();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @GetMapping("/{buildingId}")
    public Building getBuilding(@PathVariable int buildingId) {
        try {
            return buildingDao.getBuildingById(buildingId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @PostMapping
    public void addBuilding(@RequestBody Building building) {
        try {
            buildingDao.addBuilding(building);
            logger.info(String.format("Adding building %s", building.getName()));
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @PutMapping("/{buildingId}")
    public void updateBuilding(@PathVariable int buildingId, @RequestBody Building building) {
        try {
            building.setBuildingId(buildingId);
            buildingDao.updateBuilding(building);
            logger.info(String.format("Updating building %d", buildingId));
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @DeleteMapping("/{buildingId}")
    public void deleteAuthority(@PathVariable int buildingId) {
        try {
            buildingDao.deleteBuilding(buildingId);
            logger.info(String.format("Removing building %d", buildingId));
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }
}