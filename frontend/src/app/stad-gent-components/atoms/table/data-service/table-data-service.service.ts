import { Injectable } from '@angular/core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { LocationService } from 'src/app/services/api/locations/location.service';
import { Location } from 'src/app/shared/model/Location';
import { LocationReservation, LocationReservationState } from 'src/app/shared/model/LocationReservation';
import { User } from 'src/app/shared/model/User';
import { TabularData } from '../tabular-data';

@Injectable({
  providedIn: 'root',
})
export class TableDataService {
  constructor(private locationService: LocationService) {}

  isLocationScannable(location: Location) {
    return (
      location.currentTimeslot &&
      location.currentTimeslot.reservable &&
      location.currentTimeslot.isCurrent()
    );
  }

  locationsToScannable(locations: Location[]): TabularData<Location> {
    return {
      columns: [
        {
          columnHeader: 'scan.locations.header.name',
          type: 'contentColumn',
          columnContent: (l) => l.name,
          width: 50,
        },
        {
          columnHeader: 'scan.locations.header.building',
          type: 'contentColumn',
          columnContent: (l) => l.building.name,
          width: 30,
        },
        {
          columnHeader: 'scan.locations.header.numberOfSeats',
          type: 'contentColumn',
          columnContent: (l) => `${l.numberOfSeats}`,
          width: 10,
        },
        {
          columnHeader: 'scan.locations.header.scan',
          type: 'actionColumn',
          width: 10,
          columnContent: (l) =>
            this.isLocationScannable(l)
              ? {
                  actionType: 'icon',
                  actionContent: 'hamburger',
                  fallbackContent: 'Scan',
                }
              : {
                  actionType: 'string',
                  actionContent: 'Closed',
                  disabled: true,
                },
        },
      ],
      data: locations,
    };
  }

  usersToTable(users: User[], icon = "icon-hamburger"): TabularData<User> {
    return {
      columns: [
        {
          columnHeader: "management.users.searchResult.table.id",
          type: 'contentColumn',
          columnContent: user => user.userId,
        }, {
          columnHeader: "management.users.searchResult.table.firstName",
          type: 'contentColumn',
          columnContent: user => user.firstName

        }, {
          columnHeader: "management.users.searchResult.table.lastName",
          type: 'contentColumn',
          columnContent: user => user.lastName

        }, {
          columnHeader: "management.users.searchResult.table.institution",
          type: 'contentColumn',
          columnContent: user => user.institution
        },
        {
          columnHeader: "",
          type: 'actionColumn',
          width: 7,
          columnContent: user => ({
            actionType: 'icon',
            actionContent: icon.replace("icon-", ""),
            fallbackContent: 'Details',
          })
        },
      ],
      data: users
    }
  }

  reservationsToScanningTable(reservations: LocationReservation[]): TabularData<LocationReservation> {
    return {
      columns: [
        {
          columnHeader: "management.locationDetails.calendar.reservations.table.user",
          type: 'contentColumn',
          columnContent: lr => `${lr.user.firstName} ${lr.user.lastName}`,
        }, {
          columnHeader: "management.locationDetails.calendar.reservations.table.attended",
          type: 'contentColumn',
          columnContent: _ => "",
          translateColumnContent: lr => this.getCorrectI18NObjectOfScan(lr),
        },
        {
          columnHeader: "management.locationDetails.calendar.reservations.table.scan",
          type: 'actionColumn',
          width: 7,
          columnContent: lr => ({
            actionType: 'button',
            actionContent: "Yes",
            buttonClass: "primary",
            disabled: this.disableYesButton(lr)
          })
        },
        {
          columnHeader: "",
          type: 'actionColumn',
          width: 7,
          columnContent: lr => ({
            actionType: 'button',
            actionContent: "No",
            buttonClass: "secondary",
            disabled: this.disableNoButton(lr)
          })
        },
      ],
      data: reservations
    }
  }

  private getCorrectI18NObjectOfScan(reservation: LocationReservation): string {
    switch (reservation.state) {
      case LocationReservationState.PRESENT: {
        return 'general.yes';
      }
      case LocationReservationState.ABSENT: {
        return 'general.no';
      }
      default: {
        if (!reservation.timeslot.isInPast()) {
          return "general.notAvailableAbbreviation";
        } else {
          return "management.locationDetails.calendar.reservations.table.notScanned";
        }
      }
    }
  }

  private disableYesButton(reservation: LocationReservation): boolean {
    return reservation.state === LocationReservationState.PRESENT;
  }

  private disableNoButton(reservation: LocationReservation): boolean {
    return reservation.state === LocationReservationState.ABSENT;
  }

  reservationsToProfileTable(reservations: LocationReservation[]): Observable<TabularData<LocationReservation>> {
    const sreservations = Array.from(reservations).sort((a, b) => a.timeslot.getStartMoment().isBefore(b.timeslot.getStartMoment()) ? 1:-1)
    const locations = sreservations.map(r => this.locationService.getLocation(r.timeslot.locationId))
    return combineLatest(locations).pipe(
      map( locs => new Map(locs.map(l => [l.locationId, l]))),
      map(locs => (
        {
          columns: [
            {
              columnHeader: "profile.reservations.locations.table.header.locationName",
              type: 'contentColumn',
              columnContent: lr => locs.get(lr.timeslot.locationId).name,
            }, {
              columnHeader: "profile.reservations.locations.table.header.reservationDate",
              type: 'contentColumn',
              columnContent: lr => lr.timeslot.timeslotDate.format("DD/MM/YYYY"),
            },
            {
              columnHeader: "profile.reservations.locations.table.header.beginHour",
              type: 'contentColumn',
              columnContent: _ => "",
              translateColumnContent: lr => lr.timeslot.openingHour.format("HH:mm"),
            },
            {
              columnHeader: "profile.reservations.locations.table.header.state",
              type: 'contentColumn',
              columnContent: _ => "",
              translateColumnContent: lr => 'profile.reservations.locations.table.attended.' + lr.state,
            },

            {
              columnHeader: "",
              type: 'actionColumn',
              width: 7,
              columnContent: lr => ({
                actionType: 'icon',
                actionContent: "cross",
                fallbackContent: "Delete",
                disabled: !this.canDeleteReservation(lr)
              })
            },
          ],
          data: sreservations
        }
      ))
    )
  }

  private canDeleteReservation(
    reservation: LocationReservation
  ): boolean {
    switch (reservation.state) {
      case LocationReservationState.PENDING:
      case LocationReservationState.REJECTED:
      case LocationReservationState.APPROVED: {
        return true;
      }
      default: {
        return false;
      }
    }
  }
}
