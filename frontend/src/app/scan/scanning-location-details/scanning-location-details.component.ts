import { Component, OnInit } from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {LocationService} from '../../services/api/locations/location.service';
import {Observable, Subject} from 'rxjs';
import {Location} from '../../shared/model/Location';
import {User} from '../../shared/model/User';
import {ScanningService} from '../../services/api/scan/scanning.service';
import {catchError} from 'rxjs/operators';
import {of} from 'rxjs/internal/observable/of';

@Component({
  selector: 'app-scanning-location-details',
  templateUrl: './scanning-location-details.component.html',
  styleUrls: ['./scanning-location-details.component.css']
})
export class ScanningLocationDetailsComponent implements OnInit {

  locationObs: Observable<Location>;
  usersObs: Observable<User[]>;
  loadingError = new Subject<boolean>();
  user?: User;

  constructor(private route: ActivatedRoute,
              private locationService: LocationService,
              private scanningService: ScanningService) { }

  ngOnInit(): void {
    const locationId = Number(this.route.snapshot.paramMap.get('locationId'));
    // thanks to the caching that was implemented, the locationService will just return the cached location
    this.locationObs = this.locationService.getLocation(locationId);

    this.usersObs = this.scanningService.getUsersForLocationToScan(locationId).pipe(
      catchError(err => {
        console.error('Error while loading the users you could scan.', err);
        this.loadingError.next(true);
        return of(null);
      })
    );
  }

  getValidator(users: User[]): (a: string) => boolean {
    return (code) => {console.log(code, users); return users.some(u => u.augentID === this.upcEncoded(code))};
  }

  private upcEncoded(code: string): string {
    return '0' + code.substr(0, code.length - 1);
  }

  scanUser(users: User[], code: string): void {
    const user = users.find(u => u.augentID === this.upcEncoded(code));
    this.user = user;
  }

  confirm(): void {

  }
}
