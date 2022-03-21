package blok2.stadgent.model;

import blok2.daos.services.TimeslotService;
import blok2.model.calendar.Timeslot;
import blok2.model.reservables.Location;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StadGentLocation {
    public static String baseUrl = "";

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("titel")
    private String name;

    @JsonProperty("teaser_img_url")
    private String teaserUrl;

    @JsonProperty("adres")
    private String adres;

    @JsonProperty("postcode")
    private String postcode = "";

    @JsonProperty("gemeente")
    private String gemeente = "";

    @JsonProperty("totale_capaciteit")
    private Integer capacity;

    @JsonProperty("bezetting")
    private Integer reserved;

    private boolean isReservable;

    @JsonProperty("tag_1")
    public String getReservationMethod() {
        return isReservable ? "Reserveerbaar" : "Vrij toegankelijk";
    }

    private Optional<Timeslot> optionalNextUpcomingReservableTimeslot;

    @JsonProperty("tag_2")
    public String getStartDateReservation() {
        if (optionalNextUpcomingReservableTimeslot.isPresent()) {
            Timeslot nextUpcomingReservableTimeslot = optionalNextUpcomingReservableTimeslot.get();
            return "Reservatie voor week " + nextUpcomingReservableTimeslot.timeslotDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " open vanaf " + nextUpcomingReservableTimeslot.getReservableFrom().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        } else {
            return "";
        }
    }

    private boolean tomorrowStillAvailable;
    private boolean openDuringWeek;
    private boolean openDuringWeekend;

    @JsonProperty("tag_3")
    public String getAvailability() {
        ArrayList<String> tags = new ArrayList<>();
        if (tomorrowStillAvailable) {
            tags.add("Morgen nog beschikbaar");
        }
        if (openDuringWeek) {
            tags.add("Week");
        }
        if (openDuringWeekend) {
            tags.add("Weekend");
        }

        return String.join(", ", tags);
    }

    @JsonProperty("label_1")
    private String buildingName;

    @JsonProperty("lees_meer")
    public String getUrl() {
        return baseUrl + "dashboard/" + id;
    }

    @JsonProperty("openingsuren")
    public String hours;

    private Double lat;
    private Double lng;

    @JsonProperty("coordinates")
    public String getCoordinates() {
        DecimalFormat format = new DecimalFormat("###.####");
        return format.format(lat) + ", " + format.format(lng);
    }

    @JsonProperty("datum_reservatie")
    public String date;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTeaserUrl() {
        return teaserUrl;
    }

    public String getAdres() {
        return adres;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getGemeente() {
        return gemeente;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getReserved() {
        return reserved;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public StadGentLocation(Integer id, String name, String teaserUrl, String adres, String postcode, String gemeente, Integer capacity, Integer reserved, boolean isReservable, String buildingName, String hours, Double lat, Double lng, String date, boolean tomorrowStillAvailable, boolean openDuringWeek, boolean openDuringWeekend, Optional<Timeslot> optionalNextUpcomingReservableTimeslot) {
        this.id = id;
        this.name = name;
        this.teaserUrl = (teaserUrl == null || teaserUrl.trim().equals("")) ? getRandomUrl() : teaserUrl;
        this.adres = adres;
        this.postcode = postcode;
        this.gemeente = gemeente;
        this.capacity = capacity;
        this.reserved = reserved;
        if (reserved == null)
            this.reserved = 0;
        this.isReservable = isReservable;
        this.buildingName = buildingName;
        this.hours = hours;
        this.lat = lat;
        this.lng = lng;
        this.date = date;
        this.tomorrowStillAvailable = tomorrowStillAvailable;
        this.openDuringWeek = openDuringWeek;
        this.openDuringWeekend = openDuringWeekend;
        this.optionalNextUpcomingReservableTimeslot = optionalNextUpcomingReservableTimeslot;
    }

    public static StadGentLocation fromLocation(Location loc, TimeslotService ts) {
        Integer amountOfReservations = loc.getCurrentTimeslot() == null ? null : loc.getCurrentTimeslot().getAmountOfReservations();
        boolean isReservable = loc.getCurrentTimeslot() != null && loc.getCurrentTimeslot().isReservable();

        LocalDate date = LocalDate.now();
        List<Timeslot> timeslotsOfLocation = ts.getTimeslotsOfLocation(loc.getLocationId());
        Stream<Timeslot> l = timeslotsOfLocation.stream().filter(t -> t.timeslotDate().isEqual(date));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String hours = l.map(t -> t.getOpeningHour().format(formatter) + " - " + t.getClosingHour().format(formatter)).collect(Collectors.joining(","));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        String dateStr = loc.getCurrentTimeslot() == null ? null : loc.getCurrentTimeslot().timeslotDate().format(dateTimeFormatter);
        LocalTime openingTime = loc.getCurrentTimeslot() == null ? null : loc.getCurrentTimeslot().getOpeningHour();
        LocalTime closingTime = loc.getCurrentTimeslot() == null ? null : loc.getCurrentTimeslot().getClosingHour();

        boolean tomorrowStillAvailable = timeslotsOfLocation.stream().filter(timeslot -> timeslot.timeslotDate().isEqual(LocalDate.now().plusDays(1))).anyMatch(timeslot -> timeslot.getAmountOfReservations() < timeslot.getSeatCount());
        boolean openDuringWeek = timeslotsOfLocation.stream().filter(timeslot -> timeslot.timeslotDate().isAfter(date) && timeslot.timeslotDate().isBefore(date.plusDays(8))).anyMatch(timeslot -> timeslot.timeslotDate().getDayOfWeek() != DayOfWeek.SATURDAY && timeslot.timeslotDate().getDayOfWeek() != DayOfWeek.SUNDAY);
        boolean openDuringWeekend = timeslotsOfLocation.stream().filter(timeslot -> timeslot.timeslotDate().isAfter(date) && timeslot.timeslotDate().isBefore(date.plusDays(8))).anyMatch(timeslot -> timeslot.timeslotDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeslot.timeslotDate().getDayOfWeek() == DayOfWeek.SUNDAY);

        Optional<Timeslot> optionalNextUpcomingReservableTimeslot = timeslotsOfLocation.stream().filter(timeslot -> timeslot.isReservable() && timeslot.getReservableFrom().isAfter(LocalDateTime.now())).min(Comparator.comparing(Timeslot::timeslotDate));

        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857");
            MathTransform t = CRS.findMathTransform(sourceCRS, targetCRS);
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
            Point point = geometryFactory.createPoint(new Coordinate(loc.getBuilding().getLatitude(), loc.getBuilding().getLongitude()));
            Point targetPoint = (Point) JTS.transform(point, t);

            return new StadGentLocation(
                    loc.getLocationId(),
                    loc.getName(),
                    loc.getImageUrl(),
                    loc.getBuilding().getAddress(),
                    "",
                    "",
                    loc.getNumberOfSeats(),
                    amountOfReservations,
                    isReservable,
                    loc.getBuilding().getName(),
                    hours,
                    targetPoint.getX(),
                    targetPoint.getY(),
                    dateStr,
                    tomorrowStillAvailable,
                    openDuringWeek,
                    openDuringWeekend,
                    optionalNextUpcomingReservableTimeslot
            );

        } catch (FactoryException | TransformException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getRandomUrl() {
        List<String> possibilities = Arrays.asList(
                "teaser1.jpg",
                "teaser2.jpg",
                "teaser3.jpg",
                "teaser4.jpg"
        );

        Random rand = new Random();
        String randomElement = possibilities.get(rand.nextInt(possibilities.size()));

        return String.format("https://studieplekken.ugent.be/assets/images/teaser/%s", randomElement);
    }
}