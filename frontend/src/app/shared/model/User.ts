export interface User {
  augentID: string;
  firstName: string;
  lastName: string;
  mail: string;
  password: string;
  penaltyPoints: number;
  institution: string;
  admin: boolean;
}

export class UserConstructor {
  static new(): User {
    return {
      augentID: '',
      firstName: '',
      lastName: '',
      mail: '',
      password: '',
      penaltyPoints: 0,
      institution: '',
      admin: false,
    };
  }

  static newFromObj(obj: User): User {
    if (obj === null) {
      return null;
    }

    return {
      augentID: obj.augentID,
      firstName: obj.firstName,
      lastName: obj.lastName,
      mail: obj.mail,
      password: obj.password,
      penaltyPoints: obj.penaltyPoints,
      institution: obj.institution,
      admin: obj.admin,
    };
  }
}
