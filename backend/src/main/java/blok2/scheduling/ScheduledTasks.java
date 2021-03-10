package blok2.scheduling;

import blok2.daos.ILocationDao;
import blok2.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Arrays;
import java.util.Map;

@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private final ILocationDao locationDao;
    private final MailService mailService;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Autowired
    public ScheduledTasks(ILocationDao locationDao, MailService mailService) {
        this.locationDao = locationDao;
        this.mailService = mailService;
    }

    /**
     * Schedule this task to be run every monday at 2 AM. The task is responsible for sending
     * an email to following UGent services:
     *     - Alarmbeheer - alarmbeheer@ugent.be
     *     - Permanentie - permanentiecentrum@ugent.be
     *     - Schoonmaak - schoonmaak@ugent.be
     *     - Veiligheid - veiligheid@ugent.be
     *
     * The mail is only sent if there are any locations that should be opened in 2 weeks from now().
     * There is a reason behind "2 weeks" from now and not "3 weeks". Every calendar period is locked for
     * updates by employees if the starts_at is less than 3 weeks from now. The mail we are sending
     * here is to notify the UGent services about all those locations that had been locked for updates
     * during last week. Therefore we need to get the overview of opening hours for locations 2 weeks
     * from now.
     */
    @Scheduled(cron = "0 0 2 * * MON")
    public void weeklyOpeningHoursMailing() {
        String[] recipients = new String[]{
                "alarmbeheer@ugent.be",
                "permanentie@ugent.be",
                "schoonmaak@ugent.be",
                "veiligheid@ugent.be"
        };

        // This is really important! Otherwise development mails could be sent to the recipients...
        if (activeProfile.contains("dev")) {
            logger.info("Not executing scheduled task weeklyOpeningHoursMailing() due to development profile.");
            return;
        }

        logger.info(String.format("Running scheduled task weeklyOpeningHoursMailing() with recipients %s", Arrays.toString(recipients)));
        try {
            LocalDate now = LocalDate.now();
            int year = now.getYear();
            int week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

            year = week > 50 ? year + 1 : year;
            week = week > 50 ? (week + 2) % 52 : week + 2;

            Map<String, String[]> openingHours = locationDao.getOpeningOverviewOfWeek(year, week);
            System.out.println(openingHours.size() + " week " + week + " year" + year);
            if (openingHours.size() > 0) {
                logger.info(String.format("Sending mail for scheduled tast weeklyOpeningHoursMailing() because in week %d of year %d, there are %d locations that have to be opened.", week, year, openingHours.size()));
                mailService.sendOpeningHoursOverviewMail(recipients, year, week);
            } else {
                logger.info(String.format("No mail is sent for scheduled task weeklyOpeningHoursMailing() because in week %d of year %d, there will be no locations opened.", week, year));
            }
        } catch (Exception e) {
            logger.error(String.format("The scheduled task weeklyOpeningHoursMailing() could " +
                    "not be executed due to an exception that was thrown: %s", e.getMessage()));
        }
    }

}
