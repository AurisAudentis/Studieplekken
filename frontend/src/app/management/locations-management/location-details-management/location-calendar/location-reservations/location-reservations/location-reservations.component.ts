import {
  Component,
  EventEmitter,
  Input,
  Output,
  TemplateRef
} from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import * as moment from 'moment';
import { BarcodeService } from 'src/app/services/barcode.service';
import { User } from 'src/app/shared/model/User';
import { LocationReservationsService } from '../../../../../../services/api/location-reservations/location-reservations.service';
import { LocationReservation, LocationReservationState } from '../../../../../../shared/model/LocationReservation';
import {
  Timeslot,
} from '../../../../../../shared/model/Timeslot';

@Component({
  selector: 'app-location-reservations',
  templateUrl: './location-reservations.component.html',
  styleUrls: ['./location-reservations.component.scss'],
})
export class LocationReservationsComponent {
  @Input() locationReservations: LocationReservation[];
  @Input() currentTimeSlot: Timeslot;
  @Input() lastScanned?: LocationReservation;

  @Input() isManagement = true; // enable some functionality that should not be enabled for volunteers in the Scan page

  @Output()
  reservationChange: EventEmitter<unknown> = new EventEmitter<unknown>();

  locationReservationToDelete: LocationReservation = undefined;

  successDeletingLocationReservation: boolean = undefined;

  searchTerm = '';

  scannedLocationReservations: LocationReservation[] = [];
  noSuchUserFoundWarning = false;
  waitingForServer = false;

  selectionTimeout: number;
  penaltyManagerUser: User;

  userHasSearchTerm: (u: User) => boolean = (u: User) =>
    u.userId.includes(this.searchTerm) ||
    u.firstName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
    u.lastName.toLowerCase().includes(this.searchTerm.toLowerCase())

  constructor(
    private locationReservationService: LocationReservationsService,
    private modalService: MatDialog,
    private barcodeService: BarcodeService
  ) {}

  // /***************
  // *   SCANNING   *
  // ****************/

  scanLocationReservation(
    reservation: LocationReservation,
    attended: boolean,
    errorTemplate: TemplateRef<unknown>
  ): void {
    const idx = this.scannedLocationReservations.findIndex((r) => {
      return (
        r.timeslot.timeslotSequenceNumber === reservation.timeslot.timeslotSequenceNumber &&
        r.user === reservation.user
      );
    });

    const oldState = idx < 0? undefined : this.scannedLocationReservations[idx].state;
    const newState = attended? LocationReservationState.PRESENT : LocationReservationState.ABSENT;
    // only perform API call if the attendance/absence changes
    if (
      oldState === newState
    ) {
      return;
    }

    this.waitingForServer = true;
    this.locationReservationService
      .postLocationReservationAttendance(reservation, attended)
      .subscribe(
        () => {
          this.waitingForServer = false;
          reservation.state = newState;

          if (idx < 0) {
            this.scannedLocationReservations.push(reservation);
          } else {
            this.scannedLocationReservations[idx].state = newState;
          }
        },
        (err) => {
          this.waitingForServer = false;
          console.error(err);
          this.modalService.open(errorTemplate);
        }
      );
  }

  onFinishScanningClick(modalTemplate: TemplateRef<unknown>): void {
    this.modalService.open(modalTemplate, { panelClass: ["cs--cyan", "bigmodal"] });
  }

  setAllNotScannedToUnattended(errorTemplate: TemplateRef<unknown>): void {
    // hide finishScanningModal
    this.modalService.closeAll();

    // if the update is not successful, rollback UI changes
    const newLocationReservations: LocationReservation[] = [];

    // set all reservations where attended is null to false
    this.locationReservations.forEach((reservation) => {
      if (reservation.state !== LocationReservationState.PRESENT) {
        newLocationReservations.push(new LocationReservation(reservation.user, reservation.timeslot, LocationReservationState.ABSENT, reservation.createdAt));
      } else {
        newLocationReservations.push(reservation);
      }
    });

    // update server side
    this.locationReservationService
      .setAllNotScannedAsUnattended(this.currentTimeSlot)
      .subscribe(
        () => {
          this.locationReservations = newLocationReservations;
        },
        () => {
          this.modalService.open(errorTemplate, { panelClass: ["cs--cyan", "bigmodal"] });
        }
      );
  }

  // /*************
  // *   DELETE   *
  // **************/

  prepareToDeleteLocationReservation(
    locationReservation: LocationReservation,
    template: TemplateRef<unknown>
  ): void {
    this.successDeletingLocationReservation = undefined;
    this.locationReservationToDelete = locationReservation;
    this.modalService.open(template, { panelClass: ["cs--cyan", "bigmodal"] });
  }

  deleteLocationReservation(): void {
    this.successDeletingLocationReservation = null;
    this.locationReservationService
      .deleteLocationReservation(this.locationReservationToDelete)
      .subscribe(() => {
        this.successDeletingLocationReservation = true;
        if (this.reservationChange) {
          this.reservationChange.emit(null);
        }
        this.modalService.closeAll();
      });
  }

  // /******************
  // *   AUXILIARIES   *
  // *******************/

  getCorrectI18NObject(reservation: LocationReservation): string {
    switch (reservation.state) {
      case LocationReservationState.PRESENT: {
        return 'general.yes';
      }
      case LocationReservationState.ABSENT: {
        return 'general.no';
      }
      default: {
        if (this.isTimeslotStartInFuture()) {
          return 'general.notAvailableAbbreviation';
        } else {
          return 'management.locationDetails.calendar.reservations.table.notScanned';
        }
      }
    }
  }

  isTimeslotEndInPast(): boolean {
    return this.currentTimeSlot.getEndMoment().isBefore(moment());
  }

  isTimeslotStartInPast(): boolean {

    const start = this.currentTimeSlot.getStartMoment()
    return start.isBefore(moment());
  }

  isTimeslotStartInFuture(): boolean {
    return !this.isTimeslotStartInPast();
  }

  disableYesButton(reservation: LocationReservation): boolean {
    return reservation.state === LocationReservationState.PRESENT;
  }

  disableNoButton(reservation: LocationReservation): boolean {
    return reservation.state === LocationReservationState.ABSENT;
  }

  /**
   * Update this.locationReservations based on this.scannedLocationReservations
   */
  updateLocationReservations(): void {
    this.scannedLocationReservations.forEach((slr) => {
      const idx = this.locationReservations.findIndex(
        (lr) =>
          lr.user === slr.user &&
          lr.timeslot.timeslotSequenceNumber === slr.timeslot.timeslotSequenceNumber
      );

      if (idx >= 0) {
        this.locationReservations[idx].state = slr.state;
      }
    });
  }

  closeModal(): void {
    this.modalService.closeAll();
  }

  updateSearchTerm(errorTemplate: TemplateRef<unknown>): void {
    this.noSuchUserFoundWarning =
      this.searchTerm.length > 0 &&
      this.locationReservations.every((lr) => !this.userHasSearchTerm(lr.user));
    if(this.noSuchUserFoundWarning)
      this.delayedSelectInputBox();

    const fullyMatchedUser = this.barcodeService.getReservation(
      this.locationReservations,
      this.searchTerm
    );

    if (fullyMatchedUser) {
      this.lastScanned = fullyMatchedUser;
      this.scanLocationReservation(fullyMatchedUser, true, errorTemplate);
      setTimeout(() => {
        this.searchTerm = '';
        this.updateSearchTerm(errorTemplate);
      }, 10);
    } else if(this.searchTerm.length != 0) {
      this.lastScanned = null;
    }

  }

  filter(locationReservations: LocationReservation[]): LocationReservation[] {
    locationReservations = locationReservations.filter(r => r.state !== LocationReservationState.DELETED);

    // Sorting the searchTerm hits first. After that, fallback on name sorting (createdAt is not available here)
    locationReservations.sort((a, b) => {
      if (a === this.lastScanned || b === this.lastScanned) {
        return a === this.lastScanned ? -1 : 1;
      }

      if (this.userHasSearchTerm(a.user) !== this.userHasSearchTerm(b.user)) {
        return this.userHasSearchTerm(a.user) ? -1 : 1;
      }

      if (b.state !== a.state) {
        const order = [
          LocationReservationState.APPROVED, // Not scanned first.
          LocationReservationState.ABSENT,
          LocationReservationState.PRESENT,
          LocationReservationState.REJECTED,
          LocationReservationState.PENDING
        ];
        for (const state of order) {
          if (a.state === state) {
            return -1;
          }
          if (b.state === state) {
            return 1;
          }
        }
      }

      // If a.user.firstName equals b.user.firstName, the first localeCompare returns 0 (= false)
      // and thus the second localeCompare is executed. If they are not equal, the first localeCompare
      // returns either -1 or 1 (both equivalent to 'true' in a boolean expression) and thus the second
      // localeCompare is not executed.
      return a.user.firstName.localeCompare(b.user.firstName) ||
        a.user.lastName.localeCompare(b.user.lastName);
    });

    return locationReservations;
  }

  selectInputBox() {
    const el = document.getElementById("search") as HTMLInputElement;
    el.focus();
    el.select();
  }

  delayedSelectInputBox() {
    if(this.selectionTimeout)
      clearTimeout(this.selectionTimeout);

      this.selectionTimeout = setTimeout(() => this.selectInputBox(), 800);
  }

  openPenaltyBox(locres: LocationReservation, modal: TemplateRef<unknown>) {
    this.penaltyManagerUser = locres.user;
    this.modalService.open(modal, {panelClass: ["cs--cyan", "bigmodal"]});
  }
}
