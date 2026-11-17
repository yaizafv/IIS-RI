package uo.ri.cws.domain;

import java.util.HashSet;
import java.util.Set;

import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.FullName;
import uo.ri.cws.domain.types.Nif;
import uo.ri.util.assertion.ArgumentChecks;

public class Mechanic extends BaseEntity {

	// Natural attributes
	private Nif nif;
	private FullName fullName;

	// Accidental attributes
	private Set<WorkOrder> assigned = new HashSet<>();

	Mechanic() {
		// for JPA
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

	// equals() and hashCode() are inherited from BaseEntity (artificial
	// identity): the nif can be changed by the update use case, so it cannot
	// define the identity of the mechanic. Its uniqueness is a database constraint.

}
