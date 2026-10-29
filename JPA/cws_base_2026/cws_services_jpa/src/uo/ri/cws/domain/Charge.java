package uo.ri.cws.domain;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.StateChecks;

@Entity
public class Charge extends BaseEntity {

	// Natural attributes
	@ManyToOne
	private Money amount; // cents

	// Accidental attributes
	@ManyToOne
	private Invoice invoice;
	@ManyToOne
	private PaymentMean paymentMean;

	// ------ Constructors -----

	public Charge() {
	}

	/* package */ Charge(Invoice invoice, PaymentMean paymentMean, Money amount) {
		// validates the arguments
		// store the amount rounded to cents
		// increments the paymentMean accumulated -> paymentMean.charge( amount )
		// links invoice, this and paymentMean
		ArgumentChecks.isNotNull(invoice, "Invoice cannot be null");
		ArgumentChecks.isNotNull(paymentMean, "Payment mean cannot be null");
		ArgumentChecks.isNotNull(amount, "Amount cannot be null");
		Money normalized = Money.calc(amount).toMoneyRoundedToCents();
		ArgumentChecks.isTrue(normalized.greaterThan(Money.ZERO), "Amount must be positive");

		this.amount = normalized;

		paymentMean.charge(this.amount);
		Associations.SettlesWithdraws.atomicLink(invoice, this, paymentMean);
	}

	// ---- Getters ----

	public Money getAmount() {
		return amount;
	}

	public Invoice getInvoice() {
		return invoice;
	}

	public PaymentMean getPaymentMean() {
		return paymentMean;
	}

	// ---- Bussiness methods ----

	/**
	 * Restores and the value to the payment mean and unlinks the charge from the
	 * invoice and the payment mean.
	 */
	/* package */ void rewind() {
		// decrements the payment mean accumulated ( paymentMean.rewind( amount) )
		// unlinks invoice, this and paymentMean only if the compensation succeeds
		StateChecks.isTrue(invoice != null, "Charge already rewound");

		paymentMean.rewind(amount);
		Associations.SettlesWithdraws.atomicUnlink(this);
	}

	// ---- Association helpers ----

	void _setInvoice(Invoice invoice) {
		this.invoice = invoice;
	}

	void _setPaymentMean(PaymentMean paymentMean) {
		this.paymentMean = paymentMean;
	}

	// ---- toString ----
	@Override
	public String toString() {
		return "Charge [invoice=" + invoice + ", paymentMean=" + paymentMean + ", amount=" + amount + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(invoice, paymentMean);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Charge other = (Charge) obj;
		return Objects.equals(invoice, other.invoice) && Objects.equals(paymentMean, other.paymentMean);
	}

}
