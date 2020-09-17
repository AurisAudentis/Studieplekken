import { Component, OnInit } from '@angular/core';
import {ApplicationTypeFunctionalityService} from '../services/functionality/application-type/application-type-functionality.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  showReservations: boolean;
  showPenalties: boolean;

  constructor(private functionalityService: ApplicationTypeFunctionalityService) { }

  ngOnInit(): void {
    this.showReservations = this.functionalityService.showReservationsFunctionality();
    this.showPenalties = this.functionalityService.showPenaltyFunctionality();
  }

}
