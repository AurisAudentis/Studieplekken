package blok2.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "authority")
public class Authority implements Cloneable {

    @Id
    @Column(name = "authority_id")
    private int authorityId;

    @Column(name = "authority_name")
    private String authorityName;

    @Column(name = "description")
    private String description;

    public Authority(int authorityId, String authorityName, String description) {
        this.authorityId = authorityId;
        this.authorityName = authorityName;
        this.description = description;
    }

    public Authority() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Authority authority = (Authority) o;
        return Objects.equals(authorityId, authority.authorityId) &&
                Objects.equals(authorityName, authority.authorityName) &&
                Objects.equals(description, authority.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorityId, authorityName, description);
    }

    @Override
    public Authority clone() {
        try {
            return (Authority) super.clone();
        } catch (CloneNotSupportedException ignore) {
            // will never happen (Authority implements Cloneable)
            return null;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public int getAuthorityId() {
        return authorityId;
    }

    public void setAuthorityId(int authorityId) {
        this.authorityId = authorityId;
    }

}
