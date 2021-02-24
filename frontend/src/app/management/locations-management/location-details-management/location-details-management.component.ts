import { Component, OnInit } from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {LocationDetailsService} from '../../../services/single-point-of-truth/location-details/location-details.service';
import {Observable} from 'rxjs';
import {Location} from '../../../shared/model/Location';
import {ApplicationTypeFunctionalityService} from '../../../services/functionality/application-type/application-type-functionality.service';

@Component({
  selector: 'app-location-details-management',
  templateUrl: './location-details-management.component.html',
  styleUrls: ['./location-details-management.component.css']
})
export class LocationDetailsManagementComponent implements OnInit {
  locationObs: Observable<Location> = this.locationDetailsService.locationObs;

  Object = Object;

  showLockersManagement: boolean;

  constructor(private locationDetailsService: LocationDetailsService,
              private route: ActivatedRoute,
              private functionalityService: ApplicationTypeFunctionalityService) { }

  ngOnInit(): void {
    const locationId = Number(this.route.snapshot.paramMap.get('locationId'));
    this.locationDetailsService.loadLocation(locationId);
    this.showLockersManagement = this.functionalityService.showLockersManagementFunctionality();
  }
}
