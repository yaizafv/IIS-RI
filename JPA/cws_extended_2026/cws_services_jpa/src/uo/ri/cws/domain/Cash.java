package uo.ri.cws.domain;

import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;

public class Cash extends PaymentMean {

	// ---- Constructors ----
	Cash() {
		// for JPA
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

	/**
	 * Every client always keeps its cash payment mean
	 */
	@Override
	public boolean isRemovable() {
		return false;
	}

	// ---- toString ----
	@Override
	public String toString() {
		return "Cash [accumulated=" + getAccumulated() + ", client=" + getClient() + "]";
	}

}
