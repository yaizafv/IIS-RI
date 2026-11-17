package uo.ri.cws.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Address;
import uo.ri.cws.domain.types.Email;
import uo.ri.cws.domain.types.FullName;
import uo.ri.cws.domain.types.Nif;
import uo.ri.cws.domain.types.PhoneNumber;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.StateChecks;

public class Client extends BaseEntity {
	private static final int VOUCHER_25_RECOMMENDATIONS = 3;
	private static final int VOUCHER_20_WORK_ORDERS = 3;

	// Natural attributes
	private Nif nif;
	private FullName fullName;
	private Email email;
	private PhoneNumber phone;
	private Address address;

	// ACCIDENTAL ATTRIBUTES
	private Set<Vehicle> vehicles = new HashSet<>();
	private Set<PaymentMean> paymentMeans = new HashSet<>();

	private Set<Recommendation> recommendations = new HashSet<>(); // made by this client
	private Recommendation recommendation; // received by this client

	// ---- Constructors ----
	Client() {
		// for JPA
	}

	public Client(Nif nif) {
		this(nif, new FullName("No name", "No surname"), new Email("default@example.com"),
				new PhoneNumber("+34600000000"), new Address("No street", "No city", "No zip", "No country"));
	}

	public Client(Nif nif, FullName fullName) {
		this(nif, fullName, new Email("default@example.com"), new PhoneNumber("+34600000000"),
				new Address("No street", "No city", "No zip", "No country"));
	}

	public Client(Nif nif, FullName fullName, Email email, PhoneNumber phone, Address address) {
		ArgumentChecks.isNotNull(nif, "NIF cannot be null");
		ArgumentChecks.isNotNull(fullName, "Full name cannot be null");
		ArgumentChecks.isNotNull(email, "Email cannot be null");
		ArgumentChecks.isNotNull(phone, "Phone number cannot be null");
		ArgumentChecks.isNotNull(address, "Address cannot be null");

		this.nif = nif;
		this.fullName = fullName;
		this.email = email;
		this.phone = phone;
		this.address = address;
	}

	// ---- Getters ----

	public Nif getNif() {
		return nif;
	}

	public FullName getFullName() {
		return fullName;
	}

	public Email getEmail() {
		return email;
	}

	public PhoneNumber getPhone() {
		return phone;
	}

	public Address getAddress() {
		return address;
	}

	Set<Vehicle> _getVehicles() {
		return vehicles;
	}

	public Set<Vehicle> getVehicles() {
		return new HashSet<>(vehicles);
	}

	public Set<PaymentMean> getPaymentMeans() {
		return new HashSet<>(paymentMeans);
	}

	Set<PaymentMean> _getPaymentMeans() {
		return paymentMeans;
	}

	public Set<Recommendation> getRecommendations() {
		return new HashSet<>(recommendations);
	}

	Set<Recommendation> _getRecommendations() {
		return recommendations;
	}

	/**
	 * @return a new list with the work orders of all the vehicles of the client
	 */
	public List<WorkOrder> getWorkOrders() {
		return allWorkOrders();
	}

	public Recommendation getRecommendation() {
		return recommendation;
	}

	void _setRecommendation(Recommendation recommendation) {
		this.recommendation = recommendation;
	}

	// ---- Business methods ----

	/**
	 * Updates the contact data of the client
	 */
	public void update(FullName fullName, Email email, PhoneNumber phone, Address address) {
		ArgumentChecks.isNotNull(fullName, "Full name cannot be null");
		ArgumentChecks.isNotNull(email, "Email cannot be null");
		ArgumentChecks.isNotNull(phone, "Phone number cannot be null");
		ArgumentChecks.isNotNull(address, "Address cannot be null");

		this.fullName = fullName;
		this.email = email;
		this.phone = phone;
		this.address = address;
	}

	/**
	 * Changes the nif of the client, as allowed by the update use case. The
	 * uniqueness of the new nif must be checked by the caller.
	 */
	public void changeNif(Nif nif) {
		ArgumentChecks.isNotNull(nif, "NIF cannot be null");
		this.nif = nif;
	}

	/**
	 * A client is eligible for a 20€ voucher if it has, at least, three work
	 * orders not yet used for a voucher and included in paid invoices.
	 */
	public boolean isEligibleForVoucher20() {
		return getWorkOrdersAvailableForVoucher20().size() >= VOUCHER_20_WORK_ORDERS;
	}

	/**
	 * A client is eligible for a 25€ voucher if it has, at least, one paid invoice
	 * and three unused recommendations of clients that also have paid invoices.
	 */
	public boolean isEligibleForVoucher25() {
		return hasPaidInvoices() && getRecommendationsAvailableForVoucher25().size() >= VOUCHER_25_RECOMMENDATIONS;
	}

	/**
	 * @return the recommendations not yet used for a voucher whose recommended
	 *         client has, at least, one paid invoice
	 */
	public List<Recommendation> getRecommendationsAvailableForVoucher25() {
		return recommendations.stream()
				.filter(r -> !r.isUsedForVoucher())
				.filter(r -> r.getRecommended().hasPaidInvoices())
				.toList();
	}

	/**
	 * Marks as used three of the recommendations available for a 25€ voucher
	 * 
	 * @throws IllegalStateException if the client is not eligible for the voucher
	 */
	public void markThreeRecommendationsAsUsedForVoucher25() {
		StateChecks.isTrue(isEligibleForVoucher25(), "Client is not eligible for a 25€ voucher");

		getRecommendationsAvailableForVoucher25().stream()
				.limit(VOUCHER_25_RECOMMENDATIONS)
				.forEach(Recommendation::markAsUsedForVoucher);
	}

	/**
	 * @return the work orders of the client's vehicles not yet used for a voucher
	 *         and included in a paid invoice
	 */
	public List<WorkOrder> getWorkOrdersAvailableForVoucher20() {
		return allWorkOrders().stream()
				.filter(wo -> !wo.isUsedForVoucher())
				.filter(WorkOrder::isPaid)
				.toList();
	}

	/**
	 * Marks as used three of the work orders available for a 20€ voucher
	 * 
	 * @throws IllegalStateException if there are less than three available work
	 *                               orders
	 */
	public void markThreeWorkOrdersAsUsedForVoucher20() {
		StateChecks.isTrue(isEligibleForVoucher20(), "Not enough work orders for a 20€ voucher");

		getWorkOrdersAvailableForVoucher20().stream()
				.limit(VOUCHER_20_WORK_ORDERS)
				.forEach(WorkOrder::markAsUsedForVoucher);
	}

	private boolean hasPaidInvoices() {
		return allWorkOrders().stream()
				.anyMatch(WorkOrder::isPaid);
	}

	private List<WorkOrder> allWorkOrders() {
		return vehicles.stream()
				.flatMap(v -> v._getWorkOrders().stream())
				.toList();
	}

	// -- Object methods ----

	@Override
	public String toString() {
		return "Client [nif=" + nif + ", fullName=" + fullName + ", email=" + email + ", phone=" + phone + ", address="
				+ address + "]";
	}

	// equals() and hashCode() are inherited from BaseEntity (artificial
	// identity): the nif can be changed by the update use case, so it cannot
	// define the identity of the client. Its uniqueness is a database constraint.

}