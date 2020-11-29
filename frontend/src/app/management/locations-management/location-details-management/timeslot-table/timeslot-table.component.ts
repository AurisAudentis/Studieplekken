import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import * as moment from 'moment';
import { Observable } from 'rxjs';
import { map } from 'rxjs/internal/operators/map';
import { filter } from 'rxjs/operators';
import { CalendarPeriodsService } from 'src/app/services/api/calendar-periods/calendar-periods.service';
import { LocationReservationsService } from 'src/app/services/api/location-reservations/location-reservations.service';
import { CalendarPeriod } from 'src/app/shared/model/CalendarPeriod';
import { LocationReservation } from 'src/app/shared/model/LocationReservation';
import { Timeslot, timeslotEndHour, timeslotStartHour } from 'src/app/shared/model/Timeslot';

@Component({
  selector: 'app-timeslot-table',
  templateUrl: './timeslot-table.component.html',
  styleUrls: ['./timeslot-table.component.css']
})
export class TimeslotTableComponent implements OnInit {
  timeslot: Timeslot;
  locationReservations: Observable<LocationReservation[]>;
  locationName: string;
  calendarPeriodO: Observable<CalendarPeriod>;

  constructor(private route: ActivatedRoute,
              private locationReservationsService: LocationReservationsService,
              private calendarPeriodService: CalendarPeriodsService) { }

  ngOnInit(): void {
    this.locationName = this.route.snapshot.paramMap.get('locationName');
    const calendarId = Number(this.route.snapshot.paramMap.get('calendarid'));
    const date = moment(this.route.snapshot.paramMap.get('date'), 'YYYY-MM-DD');
    const seqnr = Number(this.route.snapshot.paramMap.get('seqnr'));

    this.timeslot = new Timeslot(seqnr, date, calendarId, null);
    this.locationReservations = this.locationReservationsService.getLocationReservationsOfTimeslot(this.timeslot);
    this.calendarPeriodO = this.calendarPeriodService.getCalendarPeriodsOfLocation(this.locationName)
                                .pipe(
                                  map(x => x.filter(c => c.id === calendarId)),
                                  filter(x => x.length > 0),
                                  map(x => x[0])
                                );

  }
  print(): void {
    window.print();
  }

  timestring(timeslot: Timeslot, calendarPeriod: CalendarPeriod): string {
    return `${timeslotStartHour(calendarPeriod, timeslot).format('DD/MM/YYYY\tHH:mm')}-${timeslotEndHour(calendarPeriod, timeslot).format('DD/MM/YYYY\tHH:mm')}`;
  }
}
