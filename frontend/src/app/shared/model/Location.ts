import {LocationTag} from './LocationTag';
import {Authority, AuthorityConstructor} from './Authority';

export interface Location {
  name: string;
  address: string;
  numberOfSeats: number;
  numberOfLockers: number;
  forGroup: boolean;
  imageUrl: string;
  authority: Authority;
  descriptionDutch: string;
  descriptionEnglish: string;
  assignedTags: LocationTag[];
}

export class LocationConstructor {
  static new(): Location {
    return {
      name: '',
      address: '',
      numberOfSeats: 0,
      numberOfLockers: 0,
      forGroup: false,
      imageUrl: '',
      authority: AuthorityConstructor.new(),
      descriptionDutch: '',
      descriptionEnglish: '',
      assignedTags: []
    };
  }

  static newFromObj(obj: Location): Location {
    if (obj === null) {
      return null;
    }

    return {
      name: obj.name,
      address: obj.address,
      numberOfSeats: obj.numberOfSeats,
      numberOfLockers: obj.numberOfLockers,
      imageUrl: obj.imageUrl,
      authority: AuthorityConstructor.newFromObj(obj.authority),
      descriptionDutch: obj.descriptionDutch,
      descriptionEnglish: obj.descriptionEnglish,
      assignedTags: obj.assignedTags,
      forGroup: obj.forGroup,
    };
  }
}
