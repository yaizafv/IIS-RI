package uo.ri.cws.domain;

import java.util.Objects;

import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;

public class VehicleType extends BaseEntity {

	private String name;
	private Money pricePerHour;

	// --- Constructor --
	VehicleType() {
		// for JPA
	}

	public VehicleType(String name, Money pricePerHour) {
		ArgumentChecks.isNotBlank(name, "Vehicle type name cannot be empty");
		ArgumentChecks.isNotNull(pricePerHour, "Price per hour cannot be null");
		ArgumentChecks.isTrue(pricePerHour.isNonNegative(), "Price per hour must be non-negative");

		this.name = name;
		this.pricePerHour = Money.calc(pricePerHour).toMoneyRoundedToCents();

	}

	// --- Getters ---

	public String getName() {
		return name;
	}

	public Money getPricePerHour() {
		return pricePerHour;
	}

	// --- Object methods ---

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VehicleType other = (VehicleType) obj;
		return Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "VehicleType [name=" + name + ", pricePerHour=" + pricePerHour + "]";
	}

}
