package uo.ri.cws.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Address;
import uo.ri.cws.domain.types.Email;
import uo.ri.cws.domain.types.FullName;
import uo.ri.cws.domain.types.Nif;
import uo.ri.cws.domain.types.PhoneNumber;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.StateChecks;

@Entity
public class Client extends BaseEntity {
	private static final int VOUCHER_25_RECOMMENDATIONS = 3;
	private static final int VOUCHER_20_WORK_ORDERS = 3;

	// Natural attributes
	@Column(unique = true)
	private Nif nif;
	private FullName fullName;
	private Email email;
	private PhoneNumber phone;
	private Address address;

	// ACCIDENTAL ATTRIBUTES
	@OneToMany(mappedBy = "client")
	private Set<Vehicle> vehicles = new HashSet<>();
	@ManyToOne
	private Set<PaymentMean> paymentMeans = new HashSet<>();
	@ManyToOne
	private Set<Recommendation> recommendations = new HashSet<>(); // made by this client
	@ManyToOne
	private Recommendation recommendation; // received by this client

	// ---- Constructors ----
	public Client() {

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

	public Recommendation getRecommendation() {
		return recommendation;
	}

	void _setRecommendation(Recommendation recommendation) {
		this.recommendation = recommendation;
	}

	// ---- Business methods ----

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
		return recommendations.stream().filter(r -> !r.isUsedForVoucher())
				.filter(r -> r.getRecommended().hasPaidInvoices()).toList();
	}

	/**
	 * Marks as used three of the recommendations available for a 25€ voucher
	 * 
	 * @throws IllegalStateException if the client is not eligible for the voucher
	 */
	public void markThreeRecommendationsAsUsedForVoucher25() {
		StateChecks.isTrue(isEligibleForVoucher25(), "Client is not eligible for a 25€ voucher");

		getRecommendationsAvailableForVoucher25().stream().limit(VOUCHER_25_RECOMMENDATIONS)
				.forEach(Recommendation::markAsUsedForVoucher);
	}

	/**
	 * @return the work orders of the client's vehicles not yet used for a voucher
	 *         and included in a paid invoice
	 */
	public List<WorkOrder> getWorkOrdersAvailableForVoucher20() {
		return allWorkOrders().stream().filter(wo -> !wo.isUsedForVoucher())
				.filter(wo -> wo.getInvoice() != null && wo.getInvoice().isSettled()).toList();
	}

	/**
	 * Marks as used three of the work orders available for a 20€ voucher
	 * 
	 * @throws IllegalStateException if there are less than three available work
	 *                               orders
	 */
	public void markThreeWorkOrdersAsUsedForVoucher20() {
		List<WorkOrder> available = getWorkOrdersAvailableForVoucher20();
		StateChecks.isTrue(available.size() >= VOUCHER_20_WORK_ORDERS, "Not enough work orders for a 20€ voucher");

		available.stream().limit(VOUCHER_20_WORK_ORDERS).forEach(WorkOrder::markAsUsedForVoucher);
	}

	private boolean hasPaidInvoices() {
		return allWorkOrders().stream().anyMatch(wo -> wo.getInvoice() != null && wo.getInvoice().isSettled());
	}

	private List<WorkOrder> allWorkOrders() {
		return vehicles.stream().flatMap(v -> v._getWorkOrders().stream()).toList();
	}

	// -- Object methods ----

	@Override
	public String toString() {
		return "Client [nif=" + nif + ", fullName=" + fullName + ", email=" + email + ", phone=" + phone + ", address="
				+ address + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(nif);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Client other = (Client) obj;
		return Objects.equals(nif, other.nif);
	}

}