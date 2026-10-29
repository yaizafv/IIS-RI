package uo.ri.cws.domain;

import java.util.Objects;

import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.StateChecks;

/**
 * A recommendation made by a client (recommender) to another client
 * (recommended). A client can recommend many clients, but can only be
 * recommended once.
 */
public class Recommendation {

	// Natural attributes
	private boolean usedForVoucher = false;

	// Accidental attributes
	private Client recommender;
	private Client recommended;

	// ---- Constructors ----

	public Recommendation(Client recommender, Client recommended) {
		// Validate input parameters
		// A client cannot recommend itself
		// links recommender, this and recommended
		ArgumentChecks.isNotNull(recommender, "Recommender cannot be null");
		ArgumentChecks.isNotNull(recommended, "Recommended cannot be null");
		ArgumentChecks.isFalse(recommender.equals(recommended), "A client cannot recommend itself");

		Associations.Recommends.link(recommender, this, recommended);
	}

	// ---- Getters ----

	public boolean isUsedForVoucher() {
		return usedForVoucher;
	}

	public Client getRecommender() {
		return recommender;
	}

	public Client getRecommended() {
		return recommended;
	}

	// ---- Business methods ----

	public void markAsUsedForVoucher() {
		StateChecks.isFalse(usedForVoucher, "Recommendation already used for a voucher");
		usedForVoucher = true;
	}

	// ---- Association helpers ----

	void _setRecommender(Client recommender) {
		this.recommender = recommender;
	}

	void _setRecommended(Client recommended) {
		this.recommended = recommended;
	}

	// ---- Object methods ----

	@Override
	public int hashCode() {
		return Objects.hash(recommender, recommended);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Recommendation other = (Recommendation) obj;
		return Objects.equals(recommender, other.recommender) && Objects.equals(recommended, other.recommended);
	}

	@Override
	public String toString() {
		return "Recommendation [usedForVoucher=" + usedForVoucher + ", recommender=" + recommender + ", recommended="
				+ recommended + "]";
	}

}
