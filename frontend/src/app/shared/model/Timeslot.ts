import { ms } from 'date-fns/locale';
import * as moment from 'moment';
import { Moment } from 'moment';
import { CalendarPeriod } from './CalendarPeriod';

export class Timeslot {
    timeslotSeqnr: number;
    timeslotDate: Moment;
    calendarId: number;
    amountOfReservations: number;

    constructor(timeslotSeqnr: number, timeslotDate: Moment, calendarId: number, amountOfReservations: number) {
        this.timeslotSeqnr = timeslotSeqnr;
        this.timeslotDate = timeslotDate;
        this.calendarId = calendarId;
        this.amountOfReservations = amountOfReservations;
    }

    static fromJSON(json: any): Timeslot {
        return new Timeslot(json.timeslotSeqnr, moment(json.timeslotDate), json.calendarId, json.amountOfReservations);
    }

    toJSON(): object {
        return {
            timeslotSeqnr: this.timeslotSeqnr,
            timeslotDate: this.timeslotDate.format('YYYY-MM-DD'),
            calendarId: this.calendarId
        };
    }
}

export function timeslotStartHour(calendarPeriod: CalendarPeriod, timeslot: Timeslot): Moment {
  return moment(timeslot.timeslotDate.format('DD-MM-YYYY') + ' ' +
    calendarPeriod.openingTime.format('HH:mm'), 'DD-MM-YYYY HH:mm')
    .add(calendarPeriod.reservableTimeslotSize * timeslot.timeslotSeqnr, 'minutes');
}

export function timeslotEndHour(calendarPeriod: CalendarPeriod, timeslot: Timeslot): Moment {
    return moment(timeslot.timeslotDate.format('DD-MM-YYYY') + ' ' +
      calendarPeriod.openingTime.format('HH:mm'), 'DD-MM-YYYY HH:mm')
      .add(calendarPeriod.reservableTimeslotSize * (timeslot.timeslotSeqnr + 1), 'minutes');
}

export const getTimeslotsOnDay: (calendarPeriod: CalendarPeriod, date: Moment) => Timeslot[] =
                     (calendarPeriod, date) => calendarPeriod.timeslots.filter(ts => ts.timeslotDate.diff(date) < 1000, ms);

export const timeslotEquals: (t1: Timeslot, t2: Timeslot) => boolean = (t1, t2) =>
                                             t1.timeslotSeqnr === t2.timeslotSeqnr
                                             && t1.calendarId === t2.calendarId
                                              && t1.timeslotDate.isSame(t2.timeslotDate, 'day');

export const includesTimeslot: (l: Timeslot[], t: Timeslot) => boolean = (l, t) => l.some(lt => timeslotEquals(lt, t));
