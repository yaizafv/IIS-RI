package uo.ri.cws.domain;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Entity;
import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;
import jakarta.persistence.Column;

@Entity
public class SparePart extends BaseEntity {

	// Natural attributes
	private String code;
	private String description;
	@AttributeOverride(name = "amount", column = @Column(name = "unitPrice", precision = 10, scale = 2))
	private Money unitPrice;

	// --- Constructor --

	public SparePart() {
	}

	public SparePart(String code) {
		this(code, "no-description", Money.ZERO);
	}

	public SparePart(String code, String description, Money price) {
		// Argument checks
		// Round the price to cents
		ArgumentChecks.isNotBlank(code, "Code cannot be empty");
		ArgumentChecks.isNotBlank(description, "Description cannot be empty");
		ArgumentChecks.isNotNull(price, "Price cannot be null");
		ArgumentChecks.isTrue(price.isNonNegative(), "Price must be non-negative");

		this.code = code;
		this.description = description;
		this.unitPrice = Money.calc(price).toMoneyRoundedToCents();
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public Money getUnitPrice() {
		return unitPrice;
	}

	public Money getPrice() {
		return unitPrice;
	}

	// -- Object methods ---------------------------------------------------

	@Override
	public int hashCode() {
		return Objects.hash(code);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SparePart other = (SparePart) obj;
		return Objects.equals(code, other.code);
	}

	@Override
	public String toString() {
		return "SparePart [code=" + code + ", description=" + description + ", unitPrice=" + unitPrice + "]";
	}

}
