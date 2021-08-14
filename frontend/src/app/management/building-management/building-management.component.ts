import { Component, OnInit, TemplateRef } from '@angular/core';
import { Observable } from 'rxjs';
import { Building } from 'src/app/shared/model/Building';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { BuildingService } from 'src/app/services/api/buildings/buildings.service';
import { AuthenticationService } from 'src/app/services/authentication/authentication.service';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-building-management',
  templateUrl: './building-management.component.html',
  styleUrls: ['./building-management.component.scss'],
})
export class BuildingManagementComponent implements OnInit {
  buildingsObs: Observable<Building[]>;

  buildingFormGroup = new FormGroup({
    buildingId: new FormControl({ value: '', disabled: true }),
    name: new FormControl('', Validators.required.bind(this)),
    address: new FormControl('', Validators.required.bind(this)),
  });

  successGettingBuildings: boolean = undefined;
  successAddingBuilding: boolean = undefined;
  successUpdatingBuilding: boolean = undefined;
  successDeletingBuilding: boolean = undefined;
  showDelete: boolean = this.authenticationService.isAdmin();

  constructor(
    private buildingService: BuildingService,
    private modalService: MatDialog,
    private authenticationService: AuthenticationService
  ) {}

  get buildingId(): AbstractControl {
    return this.buildingFormGroup.get('buildingId');
  }

  get name(): AbstractControl {
    return this.buildingFormGroup.get('name');
  }

  get address(): AbstractControl {
    return this.buildingFormGroup.get('address');
  }

  // ********************
  // *   CRUD: Create   *
  // ********************/

  get building(): Building {
    return {
      buildingId: this.buildingId.value as number,
      name: this.name.value as string,
      address: this.address.value as string,
    };
  }

  ngOnInit(): void {
    this.buildingsObs = this.buildingService.getAllBuildings();
    this.buildingsObs.subscribe(
      () => {
        this.successGettingBuildings = true;
      },
      () => {
        this.successGettingBuildings = false;
      }
    );
  }

  // ********************
  // *   CRUD: Update   *
  // ********************/

  prepareFormGroup(building: Building): void {
    this.buildingFormGroup.setValue({
      buildingId: building.buildingId,
      name: building.name,
      address: building.address,
    });
  }

  closeModal(): void {
    this.modalService.closeAll();
  }

  // ********************
  // *   CRUD: Delete   *
  // ********************/

  prepareAdd(template: TemplateRef<unknown>): void {
    // reset the feedback boolean
    this.successAddingBuilding = undefined;

    // prepare the buildingFormGroup, note that the buildingId won't be shown
    // because this is automatically added by the database
    this.buildingFormGroup.setValue({
      buildingId: -1, // building id must be generated by backend
      name: '',
      address: '',
    });

    this.modalService.open(template);
  }

  addBuilding(): void {
    this.successAddingBuilding = null;
    this.buildingService.addBuilding(this.building).subscribe(
      () => {
        this.successAddingBuilding = true;
        // reload the buildings
        this.buildingsObs = this.buildingService.getAllBuildings();
        this.modalService.closeAll();
      },
      () => {
        this.successAddingBuilding = false;
      }
    );
  }

  // *****************
  // *   Auxiliary   *
  // *****************/

  prepareUpdate(building: Building, template: TemplateRef<unknown>): void {
    // reset the feedback boolean
    this.successUpdatingBuilding = undefined;

    // prepare the tagFormGroup
    this.prepareFormGroup(building);

    this.modalService.open(template);
  }

  updateBuildingInFormGroup(): void {
    this.successUpdatingBuilding = null;
    this.buildingService
      .updateBuilding(this.building.buildingId, this.building)
      .subscribe(
        () => {
          this.successUpdatingBuilding = true;
          // and reload the tags
          this.buildingsObs = this.buildingService.getAllBuildings();
          this.modalService.closeAll();
        },
        () => {
          this.successUpdatingBuilding = false;
        }
      );
  }

  prepareToDelete(building: Building, template: TemplateRef<unknown>): void {
    // reset the feedback boolean
    this.successDeletingBuilding = undefined;

    // prepare the tagFormGroup
    this.prepareFormGroup(building);

    this.modalService.open(template);
  }

  deleteBuildingInFormGroup(): void {
    this.successDeletingBuilding = null;
    this.buildingService.deleteBuilding(this.building.buildingId).subscribe(
      () => {
        this.successDeletingBuilding = true;
        // and reload the tags
        this.buildingsObs = this.buildingService.getAllBuildings();
        this.modalService.closeAll();
      },
      () => {
        this.successDeletingBuilding = false;
      }
    );
  }

  validBuildingFormGroup(): boolean {
    return !this.buildingFormGroup.invalid;
  }
}
