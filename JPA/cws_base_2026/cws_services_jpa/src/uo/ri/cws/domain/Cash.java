package uo.ri.cws.domain;

import jakarta.persistence.Entity;
import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;

@Entity
public class Cash extends PaymentMean {

	// ---- Constructors ----
	public Cash() {
	}

	public Cash(Client client) {
		ArgumentChecks.isNotNull(client, "Client cannot be null");

		Associations.Holds.link(client, this);
	}

	// ---- Business methods ----
	@Override
	public boolean canBeCharged(Money amount) {
		checkPositive(amount);
		return true;
	}

	// ---- toString ----
	@Override
	public String toString() {
		return "Cash [accumulated=" + getAccumulated() + ", client=" + getClient() + "]";
	}

}
