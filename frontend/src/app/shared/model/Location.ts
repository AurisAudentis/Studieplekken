import {CustomDate, CustomDateConstructor} from './helpers/CustomDate';

export interface Location {
  name: string;
  address: string;
  numberOfSeats: number;
  numberOfLockers: number;
  descriptions: {};
  imageUrl: string;
  startPeriodLockers: CustomDate;
  endPeriodLockers: CustomDate;
}

export class LocationConstructor {
  static new(): Location {
    return {
      name: '',
      address: '',
      numberOfSeats: 0,
      numberOfLockers: 0,
      descriptions: {},
      imageUrl: '',
      startPeriodLockers: CustomDateConstructor.new(),
      endPeriodLockers: CustomDateConstructor.new(),
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
      descriptions: obj.descriptions,
      imageUrl: obj.imageUrl,
      startPeriodLockers: CustomDateConstructor.newFromObj(obj.startPeriodLockers),
      endPeriodLockers: CustomDateConstructor.newFromObj(obj.endPeriodLockers),
    };
  }
}
