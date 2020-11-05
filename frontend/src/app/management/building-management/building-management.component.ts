import { Component, OnInit } from '@angular/core';
import {transition, trigger, useAnimation} from '@angular/animations';
import {rowsAnimation} from '../../shared/animations/RowAnimation';
import { Observable } from 'rxjs';
import { Building } from 'src/app/shared/model/Building';
import { AbstractControl, FormControl, FormGroup, Validators } from '@angular/forms';
import { BuildingService } from 'src/app/services/api/buildings/buildings.service';

@Component({
  selector: 'app-building-management',
  templateUrl: './building-management.component.html',
  styleUrls: ['./building-management.component.css'],
  animations: [trigger('rowsAnimation', [
    transition('void => *', [
      useAnimation(rowsAnimation)
    ])
  ])]
})
export class BuildingManagementComponent implements OnInit {
  buildingsObs: Observable<Building[]>;

  buildingFormGroup = new FormGroup({
    buildingId: new FormControl({value: '', disabled: true}),
    name: new FormControl('', Validators.required),
    address: new FormControl('', Validators.required)
  });

  successGettingBuildings: boolean = undefined;
  successAddingBuilding: boolean = undefined;
  successUpdatingBuilding: boolean = undefined;

  constructor(private buildingService: BuildingService) { }

  ngOnInit(): void {
    this.buildingsObs = this.buildingService.getAllBuildings();
    this.buildingsObs.subscribe(
      () => {
        this.successGettingBuildings = true;
      }, () => {
        this.successGettingBuildings = false;
      }
    );
  }

  prepareFormGroup(building: Building): void {
    this.buildingFormGroup.setValue({
      buildingId: building.buildingId,
      name: building.name,
      address: building.address
    });
  }

  prepareAdd(): void {
    // reset the feedback boolean
    this.successAddingBuilding = undefined;

    // prepare the buildingFormGroup, note that the buildingId won't be shown
    // because this is automatically added by the database
    this.buildingFormGroup.setValue({
      buildingId: -1, // building id must be generated by backend
      name: '',
      address: ''
    });
  }

  addBuilding(): void {
    this.successAddingBuilding = null;
    this.buildingService.addBuilding(this.building).subscribe(
      () => {
        this.successAddingBuilding = true;
        // reload the buildings
        this.buildingsObs = this.buildingService.getAllBuildings();
      }, () => {
        this.successAddingBuilding = false;
      }
    );
  }

  prepareUpdate(building: Building): void {
    // reset the feedback boolean
    this.successUpdatingBuilding = undefined;

    // prepare the tagFormGroup
    this.prepareFormGroup(building);
  }

  updateBuildingInFormGroup(): void {
    this.successUpdatingBuilding = null;
    this.buildingService.updateBuilding(this.building.buildingId, this.building).subscribe(
      () => {
        this.successUpdatingBuilding = true;
        // and reload the tags
        this.buildingsObs = this.buildingService.getAllBuildings();
      }, () => {
        this.successUpdatingBuilding = false;
      }
    );
  }

  get buildingId(): AbstractControl { return this.buildingFormGroup.get('buildingId'); }
  get name(): AbstractControl { return this.buildingFormGroup.get('name'); }
  get address(): AbstractControl { return this.buildingFormGroup.get('address'); }

  get building(): Building {
    return {
      buildingId: this.buildingId.value,
      name: this.name.value,
      address: this.address.value
    };
  }

  validBuildingFormGroup(): boolean {
    return !this.buildingFormGroup.invalid;
  }
}
