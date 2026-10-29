package uo.ri.cws.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.FullName;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;

public class Mechanic extends BaseEntity {

	// Natural attributes
	private Nif nif;
	@ManyToOne
	private FullName fullName;

	// Accidental attributes
	@OneToMany(mappedBy = "mechanic")
	private Set<WorkOrder> assigned = new HashSet<>();

	public Mechanic() {
	}

	public Mechanic(Nif nif) {
		// Delegate to the other constructor with meaningful default values for the
		// fullname
		this(nif, new FullName("No name", "No surname"));
	}

	public Mechanic(Nif nif, FullName name) {
		ArgumentChecks.isNotNull(nif, "NIF cannot be null");
		ArgumentChecks.isNotNull(name, "Full name cannot be null");

		this.nif = nif;
		this.fullName = name;
	}

	// ---- Setters (optional, controlled) ----

	public void setFullName(FullName name) {
		ArgumentChecks.isNotNull(name);
		this.fullName = name;
	}

	public void setNif(Nif nif) {
		ArgumentChecks.isNotNull(nif);
		this.nif = nif;
	}

	// ---- Associations helpers ----

	Set<WorkOrder> _getAssigned() {
		return assigned;
	}

	public Nif getNif() {
		return nif;
	}

	public FullName getFullName() {
		return fullName;
	}

	public Set<WorkOrder> getAssigned() {
		return new HashSet<>(assigned);
	}

	// ---- Object methods ----

	@Override
	public String toString() {
		return "Mechanic [nif=" + nif + ", fullName=" + fullName + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(nif);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mechanic other = (Mechanic) obj;
		return Objects.equals(nif, other.nif);
	}

}
