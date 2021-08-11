import { Component, ElementRef, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { Building } from 'src/app/shared/model/Building';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { BuildingService } from 'src/app/services/api/buildings/buildings.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { AuthenticationService } from 'src/app/services/authentication/authentication.service';
import * as Leaf from 'leaflet';
import {AddressResolverService} from "src/app/services/addressresolver/nomenatim/addressresolver.service";

// Leaflet stuff.
const iconRetinaUrl = './assets/marker-icon-2x.png';
const iconUrl = './assets/marker-icon.png';
const shadowUrl = './assets/marker-shadow.png';
const iconDefault = Leaf.icon({
  iconRetinaUrl,
  iconUrl,
  shadowUrl,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  tooltipAnchor: [16, -28],
  shadowSize: [41, 41]
});
Leaf.Marker.prototype.options.icon = iconDefault;
import { of } from 'rxjs/internal/observable/of';
import { map } from 'rxjs/internal/operators/map';

@Component({
  selector: 'app-building-management',
  templateUrl: './building-management.component.html',
  styleUrls: ['./building-management.component.css'],
})
export class BuildingManagementComponent implements OnInit {
  buildingsObs: Observable<Building[]>;
  institutionsObs: Observable<string[]>;

  buildingFormGroup = new FormGroup({
    buildingId: new FormControl({ value: '', disabled: true }),
    name: new FormControl('', Validators.required.bind(this)),
    address: new FormControl('', Validators.required.bind(this)),
    latitude: new FormControl('', Validators.required.bind(this)),
    longitude: new FormControl('', Validators.required.bind(this)),
    institution: new FormControl('', Validators.required.bind(this)),
  });

  successGettingBuildings: boolean = undefined;
  successAddingBuilding: boolean = undefined;
  successUpdatingBuilding: boolean = undefined;
  successDeletingBuilding: boolean = undefined;
  showDelete: boolean = this.authenticationService.isAdmin();

  leafletMap: Leaf.Map;
  @ViewChild('leafletMapElementAdding', { static: false }) leafletMapElementAdding: ElementRef;
  @ViewChild('leafletMapElementUpdating', { static: false }) leafletMapElementUpdating: ElementRef;

  isLoadingAddress: boolean;
  isCorrectAddress: boolean;

  constructor(
    private buildingService: BuildingService,
    private modalService: BsModalService,
    private authenticationService: AuthenticationService,
    private addressResolver: AddressResolverService
  ) {
  }

  get buildingId(): AbstractControl {
    return this.buildingFormGroup.get('buildingId');
  }

  get name(): AbstractControl {
    return this.buildingFormGroup.get('name');
  }

  get address(): AbstractControl {
    return this.buildingFormGroup.get('address');
  }

  get latitude(): AbstractControl {
    return this.buildingFormGroup.get('latitude');
  }

  get longitude(): AbstractControl {
    return this.buildingFormGroup.get('longitude');
  }
  
  get institution(): AbstractControl {
    return this.buildingFormGroup.get('institution');
  }

  // ********************
  // *   CRUD: Create   *
  // ********************/

  get building(): Building {
    return {
      buildingId: this.buildingId.value as number,
      name: this.name.value as string,
      address: this.address.value as string,
      latitude: this.latitude.value as number,
      longitude: this.longitude.value as number,
      institution: this.institution.value as string,
    };
  }

  ngOnInit(): void {
    this.buildingsObs = this.buildingService.getAllBuildings();

    // Only show the buildings the user has access to.
    if (!this.authenticationService.isAdmin()) {
      const institution = this.authenticationService.userValue().institution;
      this.buildingsObs = this.buildingsObs.pipe(
        map(items => items.filter(building => building.institution === institution))
      );
    }

    this.buildingsObs.subscribe(
      () => {
        this.successGettingBuildings = true;
      },
      () => {
        this.successGettingBuildings = false;
      }
    );
    this.fillInstitutionsDependingOnUser();
  }

  // ********************
  // *   CRUD: Update   *
  // ********************/

  prepareFormGroup(building: Building): void {
    this.buildingFormGroup.setValue({
      buildingId: building.buildingId,
      name: building.name,
      address: building.address,
      latitude: building.latitude,
      longitude: building.longitude,
      institution: building.institution,
    });
  }

  closeModal(): void {
    this.modalService.hide();
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
      latitude: 0,
      longitude: 0,
      institution: this.authenticationService.userValue().institution,
    });

    this.modalService.show(template);
  }

  addBuilding(): void {
    this.successAddingBuilding = null;
    this.buildingService.addBuilding(this.building).subscribe(
      () => {
        this.successAddingBuilding = true;
        // reload the buildings
        this.buildingsObs = this.buildingService.getAllBuildings();
        this.modalService.hide();
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

    this.isCorrectAddress = true;
    this.isLoadingAddress = false;

    this.modalService.show(template);
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
          this.modalService.hide();
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

    this.modalService.show(template);
  }

  deleteBuildingInFormGroup(): void {
    this.successDeletingBuilding = null;
    this.buildingService.deleteBuilding(this.building.buildingId).subscribe(
      () => {
        this.successDeletingBuilding = true;
        // and reload the tags
        this.buildingsObs = this.buildingService.getAllBuildings();
        this.modalService.hide();
      },
      () => {
        this.successDeletingBuilding = false;
      }
    );
  }

  validBuildingFormGroup(): boolean {
    return !this.buildingFormGroup.invalid && !this.isLoadingAddress && this.isCorrectAddress;
  }

  fillInstitutionsDependingOnUser(): void {
    if (this.authenticationService.isAdmin()) {
      this.institutionsObs = of(['UGent', 'HoGent', 'Arteveldehogeschool']);
    } else {
      this.institutionsObs = of([this.authenticationService.userValue().institution]);
    }
  }

  checkAddress() {
    const address = this.address;
    this.isLoadingAddress = true;
    this.addressResolver.query(address.value).subscribe(r => {
      this.isLoadingAddress = false;

      if(r.length == 1) {
        this.isCorrectAddress = true;
        this.buildingFormGroup.setValue(
          {
            buildingId: this.buildingId.value,
            address: this.address.value,
            name: this.name.value,
            latitude: r[0].lat,
            longitude: r[0].lon,
            institution: this.institution.value
          }
        )
      } else {
        this.isCorrectAddress = false;
      }
    })
  }

  getAddressIcon() {
    if(this.isLoadingAddress) {
      return "glyphicon-refresh waiting"
    }

    return this.isCorrectAddress ? "glyphicon-ok-circle ok": "glyphicon-remove-circle no"
  }
}
