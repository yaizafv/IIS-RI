package uo.ri.cws.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Money;
import uo.ri.cws.domain.types.Percentage;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.StateChecks;

@Entity
public class Invoice extends BaseEntity {
	private static final LocalDate JULY_1_2012 = LocalDate.of(2012, 7, 1);
	@Transient
	private static final Money VOUCHER_30_MIN_TOTAL = Money.of("500.00");

	public enum InvoiceState {
		ISSUED, PAID
	}

	// Natural attributes
	private long number;
	private LocalDate date;
	@Enumerated(EnumType.STRING)
	private InvoiceState state = InvoiceState.ISSUED;

	/*
	 * Subtotal amount of the invoice, before applying VAT. This represents the
	 * taxable base.
	 */
	@AttributeOverride(name = "amount", column = @Column(name = "SUBTOTAL", precision = 10, scale = 2))
	private Money subtotal = Money.ZERO; // cents

	/*
	 * VAT rate applied to the invoice, expressed as a percentage. Example: 21% VAT.
	 */
	@AttributeOverride(name = "value", column = @Column(name = "SUBTOTAL", precision = 10, scale = 2))
	private Percentage vatRate = Percentage.ofPercent(21); // default to 21%

	/*
	 * VAT amount calculated from the subtotal amount. This is the tax the customer
	 * must pay.
	 */
	@AttributeOverride(name = "amount", column = @Column(name = "VATRATE", precision = 10, scale = 2))
	private Money vatAmount = Money.ZERO; // cents

	/*
	 * Total amount of the invoice including VAT. This is the final amount the
	 * customer must pay.
	 */
	@AttributeOverride(name = "amount", column = @Column(name = "TOTAL", precision = 10, scale = 2))
	private Money total = Money.ZERO; // cents

	private boolean usedForVoucher = false;

	// Accidental attributes
	@OneToMany(mappedBy = "invoice")
	private Set<WorkOrder> workOrders = new HashSet<>();
	@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Charge> charges = new HashSet<>();

	// ----------------- Constructors -------------------------------------
	public Invoice() {
	}

	public Invoice(long number) {
		// call full constructor with sensible defaults
		this(number, LocalDate.now(), List.of());
	}

	public Invoice(long number, LocalDate date) {
		// call full constructor with sensible defaults
		this(number, date, List.of());
	}

	public Invoice(long number, List<WorkOrder> workOrders) {
		this(number, LocalDate.now(), workOrders);
	}

	public Invoice(long number, LocalDate date, List<WorkOrder> workOrders) {
		// check arguments (always), through IllegalArgumentException
		// store the number
		// add every work order calling addWorkOrder( w )
		ArgumentChecks.isTrue(number >= 0, "Invoice number must be positive");
		ArgumentChecks.isNotNull(date, "Invoice date cannot be null");
		ArgumentChecks.isNotNull(workOrders, "Work orders list cannot be null");
		// validate every work order before linking any, so the constructor is atomic
		workOrders.forEach(wo -> {
			ArgumentChecks.isNotNull(wo, "Work order cannot be null");
			StateChecks.isTrue(wo.isApproved(), "Work order is not approved");
		});

		this.number = number;
		this.date = date;
		this.vatRate = getApplicableVatRate();
		workOrders.forEach(this::addWorkOrder);
	}

	// ---- Getters ----

	public long getNumber() {
		return number;
	}

	public LocalDate getDate() {
		return date;
	}

	public InvoiceState getState() {
		return state;
	}

	public Money getSubtotal() {
		return subtotal;
	}

	public Percentage getVatRate() {
		return vatRate;
	}

	public Money getVatAmount() {
		return vatAmount;
	}

	public Money getTotal() {
		return total;
	}

	public boolean isUsedForVoucher() {
		return usedForVoucher;
	}

	// ------------------ Association helpers (package-private)
	// --------------------------

	Set<WorkOrder> _getWorkOrders() {
		return workOrders;
	}

	public Set<WorkOrder> getWorkOrders() {
		return new HashSet<>(workOrders);
	}

	Set<Charge> _getCharges() {
		return charges;
	}

	public Set<Charge> getCharges() {
		return new HashSet<>(charges);
	}

	// ---- Business methods --------------------------------------------

	/**
	 * Adds a work order to the invoice. The work order must be approved and the
	 * invoice must not be settled.
	 * 
	 * @param workOrder
	 */
	public void addWorkOrder(WorkOrder workOrder) {
		// Validations
		// links the work order
		// compute the invoice amount after adding the work order
		// marks it as invoiced
		ArgumentChecks.isNotNull(workOrder, "Work order cannot be null");
		StateChecks.isFalse(isSettled(), "Invoice is already settled");
		StateChecks.isTrue(workOrder.isApproved(), "Work order is not approved");

		Associations.Bills.link(this, workOrder);
		workOrder.invoice();
		computeAmount();
	}

	/**
	 * Removes a work order from the invoice. The invoice must not be settled.
	 * 
	 * @param workOrder
	 */
	public void removeWorkOrder(WorkOrder workOrder) {
		ArgumentChecks.isNotNull(workOrder, "Work order cannot be null");
		StateChecks.isFalse(isSettled(), "Invoice is already settled");
		ArgumentChecks.isTrue(workOrders.contains(workOrder), "Work order does not belong to this invoice");

		workOrder.uninvoice();
		Associations.Bills.unlink(workOrder);
		computeAmount();
	}

	public boolean isSettled() {
		return state == InvoiceState.PAID;
	}

	/**
	 * Settles the invoice by charging the specified payment means with the
	 * requested amounts. The total of the charges must equal the invoice total, and
	 * each payment mean must be able to be charged.
	 * 
	 * @param requestedCharges A map of payment means and their corresponding
	 *                         amounts to charge.
	 */
	public void settle(Map<PaymentMean, Money> requestedCharges) {
		// Validations
		// Normalize the requested charges (round to cents, check positive)
		// Check that the total of requested charges equals the invoice total
		// For each payment mean, create a Charge and link it to the invoice and payment
		// mean
		// Set the invoice state to PAID
		ArgumentChecks.isNotNull(requestedCharges, "Charges cannot be null");
		ArgumentChecks.isFalse(requestedCharges.isEmpty(), "Charges cannot be empty");
		StateChecks.isFalse(isSettled(), "Invoice is already settled");

		Map<PaymentMean, Money> charges = normalize(requestedCharges);
		StateChecks.isTrue(total.greaterThan(Money.ZERO), "An invoice with zero total cannot be settled");
		StateChecks.isTrue(chargesTotal(charges).equals(total), "Charges do not sum the invoice total");
		// check every payment mean before charging any, so the settlement is atomic
		charges.forEach((paymentMean, amount) -> StateChecks.isTrue(paymentMean.canBeCharged(amount),
				"Payment mean cannot be charged"));

		charges.forEach((paymentMean, amount) -> new Charge(this, paymentMean, amount));
		state = InvoiceState.PAID;
	}

	public void unSettle() {
		StateChecks.isTrue(isSettled(), "Invoice is not settled");

		new HashSet<>(charges).forEach(Charge::rewind);
		state = InvoiceState.ISSUED;
	}

	/**
	 * A paid invoice over 500€ not yet used for a voucher can generate a 30€
	 * voucher
	 */
	public boolean canGenerateVoucher30() {
		return isSettled() && !usedForVoucher && total.greaterThan(VOUCHER_30_MIN_TOTAL);
	}

	/**
	 * Marks the invoice as used to generate a voucher
	 * 
	 * @throws IllegalStateException if the invoice cannot generate a voucher
	 */
	public void markAsUsedForVoucher() {
		StateChecks.isTrue(canGenerateVoucher30(), "Invoice cannot generate a voucher");
		usedForVoucher = true;
	}

	// ---- Calculation ----
	private void computeAmount() {
		// 1. Sum all work order totals = subtotal
		// 2. Determine VAT rate based on invoice date
		// 3. Compute VAT amount
		// 4. Compute total amount (subtotal + VAT) rounded to cents
		Money.MoneyCalculator calculator = Money.calc();
		workOrders.forEach(wo -> calculator.add(wo.getAmount()));

		subtotal = calculator.toMoneyRoundedToCents();
		vatRate = getApplicableVatRate();
		vatAmount = Money.calc(subtotal).times(vatRate).toMoneyRoundedToCents();
		total = Money.calc(subtotal).add(vatAmount).toMoneyRoundedToCents();
	}

	private Percentage getApplicableVatRate() {
		return date.isAfter(JULY_1_2012) ? Percentage.ofPercent(21) : Percentage.ofPercent(18);
	}

	private Map<PaymentMean, Money> normalize(Map<PaymentMean, Money> requestedCharges) {
		Map<PaymentMean, Money> result = new LinkedHashMap<>();
		requestedCharges.forEach((paymentMean, amount) -> {
			ArgumentChecks.isNotNull(paymentMean);
			ArgumentChecks.isNotNull(amount);
			Money normalizedAmount = Money.calc(amount).toMoneyRoundedToCents();
			ArgumentChecks.isTrue(normalizedAmount.greaterThan(Money.ZERO), "Charge amount must be positive");
			result.put(paymentMean, normalizedAmount);
		});
		return result;
	}

	private Money chargesTotal(Map<PaymentMean, Money> requestedCharges) {
		Money.MoneyCalculator calculator = Money.calc();
		requestedCharges.values().forEach(calculator::add);
		return calculator.toMoneyRoundedToCents();
	}

	// ---- Object methods ----

	@Override
	public int hashCode() {
		return Objects.hash(Long.valueOf(number));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Invoice other = (Invoice) obj;
		return number == other.number;
	}

	@Override
	public String toString() {
		return "Invoice [number=" + number + ", date=" + date + ", state=" + state + ", subtotal=" + subtotal
				+ ", vatRate=" + vatRate + ", vatAmount=" + vatAmount + ", total=" + total + ", usedForVoucher="
				+ usedForVoucher + "]";
	}

}
