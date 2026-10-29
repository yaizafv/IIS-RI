package uo.ri.cws.domain;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;

/**
 * A substitution is a relation between an intervention and a spare part. It
 * represents the fact that a spare part has been used in an intervention.
 * 
 * Internal aggregate class of WorkOrder. Only Intervention can create
 * Substitutions (through WorkOrder).
 */
@Entity
public class Substitution extends BaseEntity {
	// Natural attributes
	@AttributeOverride(name = "amount", column = @Column(name = "PRICE", precision = 10, scale = 2))
	private Money price;
	private int quantity;

	// Accidental attributes
	@ManyToOne
	private SparePart sparePart;
	@ManyToOne
	private Intervention intervention;

	// ---------------------------- Constructors -----------------------------
	public Substitution() {
	}

	/**
	 * Package-private constructor to create a new Substitution with the given
	 * parameters. Only Intervention can create Substitutions.
	 */
	/* package */ Substitution(SparePart sparePart, Intervention intervention, int quantity) {
		// Validate input parameters
		// assignations
		// link the substitution with the intervention and spare part (atomiclink)
		ArgumentChecks.isNotNull(sparePart, "Spare part cannot be null");
		ArgumentChecks.isNotNull(intervention, "Intervention cannot be null");
		ArgumentChecks.isTrue(quantity > 0, "Quantity must be greater than zero");

		this.quantity = quantity;
		this.price = sparePart.getPrice(); // price at the moment of the substitution
		Associations.PerformsSubstitutes.atomicLink(intervention, this, sparePart);
	}

	// ---------------------------- Association helpers
	// -----------------------------

	void _setSparePart(SparePart sparePart) {
		this.sparePart = sparePart;
	}

	void _setIntervention(Intervention intervention) {
		this.intervention = intervention;
	}

	// ---------------------------- Business methods -----------------------------
	public Money getTotalAmount() {
		return Money.calc(price).times(quantity).toMoneyExact();
	}

	// ---------------------------- Getters -----------------------------

	public Money getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
	}

	public SparePart getSparePart() {
		return sparePart;
	}

	public Intervention getIntervention() {
		return intervention;
	}

	// ---------------------------- Object methods -----------------------------

	@Override
	public int hashCode() {
		return Objects.hash(intervention, sparePart);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Substitution other = (Substitution) obj;
		return Objects.equals(intervention, other.intervention) && Objects.equals(sparePart, other.sparePart);
	}

	@Override
	public String toString() {
		return "Substitution [price=" + price + ", quantity=" + quantity + ", sparePart=" + sparePart
				+ ", intervention=" + intervention + "]";
	}

}
