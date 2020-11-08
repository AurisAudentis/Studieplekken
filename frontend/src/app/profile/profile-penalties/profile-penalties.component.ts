import { Component, OnInit } from '@angular/core';
import {User} from '../../shared/model/User';
import {AuthenticationService} from '../../services/authentication/authentication.service';
import {Observable} from 'rxjs';
import {Penalty} from '../../shared/model/Penalty';
import {transition, trigger, useAnimation} from '@angular/animations';
import {rowsAnimation} from '../../shared/animations/RowAnimation';
import {objectExists} from '../../shared/GeneralFunctions';

@Component({
  selector: 'app-profile-penalties',
  templateUrl: './profile-penalties.component.html',
  styleUrls: ['./profile-penalties.component.css'],
  animations: [trigger('rowsAnimation', [
    transition('void => *', [
      useAnimation(rowsAnimation)
    ])
  ])]
})
export class ProfilePenaltiesComponent implements OnInit {
  user: User;
  penalties: Observable<Penalty[]>;


  constructor(private authenticationService: AuthenticationService) {
    authenticationService.user.subscribe(next => {
      this.user = next;
      this.penalties = authenticationService.getPenalties();
    });
  }

  ngOnInit(): void {
  }

}
