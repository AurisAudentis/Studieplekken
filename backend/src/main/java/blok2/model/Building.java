package blok2.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "buildings")
public class Building implements Cloneable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private int buildingId;

    @Column(name = "building_name")
    private String name;

    @Column(name = "address")
    private String address;

    public Building() {}

    public Building(int buildingId, String name, String address) {
        this.buildingId = buildingId;
        this.name = name;
        this.address = address;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Building)) return false;
        Building building = (Building) o;
        return buildingId == building.buildingId &&
                name.equals(building.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildingId, name);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public Building clone() {
        try {
            return (Building) super.clone();
        } catch (CloneNotSupportedException ignore) {
            // will never happen (Building implements Cloneable)
            return null;
        }
    }

}
