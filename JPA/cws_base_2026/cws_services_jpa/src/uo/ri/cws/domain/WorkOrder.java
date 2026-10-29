package uo.ri.cws.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;
import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.StateChecks;

@Entity
@Table(name = "TWORKORDERS", uniqueConstraints = @UniqueConstraint(columnNames = { "date", "vehicle_id" }))
public class WorkOrder extends BaseEntity {

	public enum WorkOrderState {
		OPEN, ASSIGNED, FINISHED, APPROVED, INVOICED
	}

	// Natural attributes
	private LocalDateTime date;
	private String description;
	@AttributeOverride(name = "amount", column = @Column(name = "AMOUNT", precision = 10, scale = 2))
	private Money amount = Money.ZERO; // cents
	private WorkOrderState state = WorkOrderState.OPEN;
	private boolean usedForVoucher = false;

	// Accidental attributes
	@ManyToOne
	private Invoice invoice;
	@ManyToOne
	private Mechanic mechanic;
	@ManyToOne
	private Vehicle vehicle;
	@OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Intervention> interventions = new HashSet<>();

	// ---- Constructors ----
	public WorkOrder() {
	}

	public WorkOrder(Vehicle vehicle) {
		this(vehicle, LocalDateTime.now(), "no-description");
	}

	public WorkOrder(Vehicle vehicle, LocalDateTime date) {
		this(vehicle, date, "no-description");
	}

	public WorkOrder(Vehicle vehicle, String description) {
		this(vehicle, LocalDateTime.now(), description);
	}

	public WorkOrder(Vehicle vehicle, LocalDateTime date, String description) {
		// Validate input parameters
		// Truncate the date to milliseconds to avoid precision issues
		// Link the work order with the vehicle

		ArgumentChecks.isNotNull(vehicle, "Vehicle cannot be null");
		ArgumentChecks.isNotNull(date, "Date cannot be null");
		ArgumentChecks.isNotBlank(description, "Description cannot be empty");

		this.date = date.truncatedTo(ChronoUnit.MILLIS);
		this.description = description;
		Associations.Fixes.link(vehicle, this);

	}

	// ---- Getters ----
	public String getDescription() {
		return description;
	}

	public Money getAmount() {
		return amount;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public Mechanic getMechanic() {
		return mechanic;
	}

	public Invoice getInvoice() {
		return invoice;
	}

	public Money getPricePerHour() {
		return getVehicle().getPricePerHour();
	}

	public Set<Intervention> getInterventions() {
		return new HashSet<>(interventions);
	}

	public WorkOrderState getState() {
		return state;
	}

	public boolean isUsedForVoucher() {
		return usedForVoucher;
	}

	// ---- Setters (only the needed ones) ----

	public void setDescription(String description) {
		ArgumentChecks.isNotBlank(description, "Description cannot be empty");
		this.description = description;
	}

	@Override
	public int hashCode() {
		return Objects.hash(date, vehicle);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WorkOrder other = (WorkOrder) obj;
		return Objects.equals(date, other.date) && Objects.equals(vehicle, other.vehicle);
	}

	@Override
	public String toString() {
		return "WorkOrder [date=" + date + ", description=" + description + ", amount=" + amount + ", state=" + state
				+ ", usedForVoucher=" + usedForVoucher + ", vehicle=" + vehicle + ", interventions=" + interventions
				+ "]";
	}

	// ---- Association helpers ----
	void _setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	void _setMechanic(Mechanic mechanic) {
		this.mechanic = mechanic;
	}

	void _setInvoice(Invoice invoice) {
		this.invoice = invoice;
	}

	Set<Intervention> _getInterventions() {
		return interventions;
	}

	// ---- State checks ----
	public boolean isOpen() {
		return state == WorkOrderState.OPEN;
	}

	public boolean isAssigned() {
		return state == WorkOrderState.ASSIGNED;
	}

	public boolean isFinished() {
		return state == WorkOrderState.FINISHED;
	}

	public boolean isApproved() {
		return state == WorkOrderState.APPROVED;
	}

	public boolean isInvoiced() {
		return state == WorkOrderState.INVOICED;
	}

	// ---- Business methods & state transitions ----

	/**
	 * Links (assigns) the work order to a mechanic and then changes its state to
	 * ASSIGNED
	 * 
	 * @see UML_State diagrams on the problem statement document
	 * @throws IllegalStateException if The work order is not in OPEN state
	 */
	public void assignTo(Mechanic mechanic) {
		ArgumentChecks.isNotNull(mechanic, "Mechanic cannot be null");
		StateChecks.isTrue(isOpen(), "Work order is not open");

		Associations.Assigns.link(mechanic, this);
		state = WorkOrderState.ASSIGNED;
	}

	/**
	 * Unlinks (unassigns) the work order from its mechanic and then changes its
	 * state to OPEN
	 * 
	 * @see UML_State diagrams on the problem statement document
	 * @throws IllegalStateException if The work order is not in ASSIGNED state
	 */
	public void unassign() {
		StateChecks.isTrue(isAssigned(), "Work order is not assigned");

		Associations.Assigns.unlink(this);
		state = WorkOrderState.OPEN;
	}

	/**
	 * Given the right conditions unlinks the workorder and the mechanic and changes
	 * the state to FINISHED. The amount is computed on approval.
	 * 
	 * @see UML_State diagrams on the problem statement document
	 * @throws IllegalStateException if the work order is not in ASSIGNED state
	 */
	public void finish() {
		StateChecks.isTrue(isAssigned(), "Work order is not assigned");

		Associations.Assigns.unlink(this);
		state = WorkOrderState.FINISHED;
	}

	/**
	 * Given the right conditions changes the state to APPROVED and computes the
	 * amount
	 * 
	 * @see UML_State diagrams on the problem statement document
	 * @throws IllegalStateException if the work order is not in FINISHED state
	 */
	public void approve() {
		StateChecks.isTrue(isFinished(), "Work order is not finished");

		computeAmount();
		state = WorkOrderState.APPROVED;
	}

	/**
	 * Given the right conditions changes the state to INVOICED
	 * 
	 * @see UML_State diagrams on the problem statement document
	 * @throws IllegalStateException if the work order is not in APPROVED state or
	 *                               if it has no invoice assigned
	 */
	public void invoice() {
		StateChecks.isTrue(isApproved(), "Work order is not approved");
		StateChecks.isNotNull(invoice, "Work order has no invoice attached");

		state = WorkOrderState.INVOICED;
	}

	/**
	 * Changes back the state to FINISHED, given the right conditions
	 * 
	 * @see UML_State diagrams on the problem statement document
	 * @throws IllegalStateException if the work order is not in APPROVED state
	 */
	public void unapprove() {
		StateChecks.isTrue(isApproved(), "Work order is not approved");

		amount = Money.ZERO;
		state = WorkOrderState.FINISHED;
	}

	/**
	 * Changes back the state to APPROVED, given the right conditions
	 * 
	 * @see UML_State diagrams on the problem statement document
	 * @throws IllegalStateException if the work order is not in INVOICED state
	 */
	public void uninvoice() {
		StateChecks.isTrue(isInvoiced(), "Work order is not invoiced");

		state = WorkOrderState.APPROVED;
	}

	/**
	 * Changes a finished work order to OPEN
	 * 
	 * @see UML_State diagrams on the problem statement document
	 * @throws IllegalStateException if the work order is not in FINISHED state
	 */
	public void reopen() {
		StateChecks.isTrue(isFinished(), "Work order is not finished");

		state = WorkOrderState.OPEN;
	}

	/**
	 * Marks the work order as used to generate a voucher
	 * 
	 * @throws IllegalStateException if it is already used or it is not included in
	 *                               a paid invoice
	 */
	public void markAsUsedForVoucher() {
		StateChecks.isFalse(usedForVoucher, "Work order already used for a voucher");
		StateChecks.isTrue(invoice != null && invoice.isSettled(), "Work order is not in a paid invoice");

		usedForVoucher = true;
	}

	// ---- Intervention management ----
	/**
	 * This method is called by the mechanic's use case AddIntervention Can only be
	 * called if the work order is assigned, and it will add an intervention for the
	 * mechanic assigned to the work order
	 * 
	 * @param date
	 * @param minutes
	 */
	public void addIntervention(LocalDateTime date, int minutes) {
		addIntervention(date, minutes, Map.of());
	}

	/**
	 * This method is called by the mechanic's use case AddIntervention Can only be
	 * called if the work order is assigned, and it will add an intervention with
	 * substitutions for the mechanic assigned to the work order
	 * 
	 * @param date
	 * @param minutes
	 * @param substitutions
	 */
	public void addIntervention(LocalDateTime date, int minutes, Map<SparePart, Integer> substitutions) {
		StateChecks.isTrue(isAssigned(), "Work order not assigned");
		addIntervention(mechanic, date, minutes, substitutions);
	}

	/**
	 * This method is called by the foreman's use case AddIntervention to mechanic
	 * Can be called if the work order is assigned or finished, and it will add an
	 * intervention
	 * 
	 * @param mechanic The mechanic performing the intervention
	 * @param date     The date and time of the intervention
	 * @param minutes  The duration of the intervention in minutes
	 */
	public void addIntervention(Mechanic mechanic, LocalDateTime date, int minutes) {
		addIntervention(mechanic, date, minutes, Map.of());
	}

	/**
	 * Adds an intervention to the work order This is a factory method that creates
	 * a new Intervention and links it to the work order and the mechanic It is
	 * called by the other addIntervention methods or from the foreman's use case
	 * 
	 * @param mechanic      The mechanic performing the intervention
	 * @param date          The date and time of the intervention
	 * @param minutes       The duration of the intervention in minutes
	 * @param substitutions A map of spare parts and their quantities used in the
	 *                      intervention
	 * @throws IllegalStateException if the work order is neither assigned nor
	 *                               finished, or if there is a duplicated
	 *                               intervention
	 */
	public void addIntervention(Mechanic mechanic, LocalDateTime date, int minutes,
			Map<SparePart, Integer> substitutions) {
		// Validate input parameters
		// State checks: work order must be assigned or finished, and no duplicated
		// intervention
		// truncate the date to milliseconds to avoid precision issues
		// Create a new Intervention (that auto links it to the work order and mechanic)
		ArgumentChecks.isNotNull(mechanic, "Mechanic cannot be null");
		ArgumentChecks.isNotNull(date, "Date cannot be null");
		ArgumentChecks.isTrue(minutes >= 0, "Minutes cannot be negative");
		ArgumentChecks.isNotNull(substitutions, "Substitutions map cannot be null");
		StateChecks.isTrue(isAssigned() || isFinished(), "Work order neither assigned nor finished");

		LocalDateTime truncated = date.truncatedTo(ChronoUnit.MILLIS);
		StateChecks.isFalse(hasIntervention(mechanic, truncated), "Duplicated intervention");

		new Intervention(mechanic, this, truncated, minutes, substitutions);
	}

	private boolean hasIntervention(Mechanic mechanic, LocalDateTime date) {
		return interventions.stream().anyMatch(i -> date.equals(i.getDate()) && mechanic.equals(i.getMechanic()));
	}

	public void removeIntervention(Intervention intervention) {
		ArgumentChecks.isNotNull(intervention);
		StateChecks.isTrue(isAssigned() || isFinished(), "Work order neither assigned nor finished");
		StateChecks.isTrue(interventions.contains(intervention), "Intervention does not belong to this work order");

		intervention.removeSubstitutions();
		Associations.RecordsIntervenes.atomicUnlink(intervention);
	}

	// ---- Amount calculation ----
	/**
	 * Sums the interventions with full precision and rounds to cents once
	 * (accounting boundary)
	 */
	private void computeAmount() {
		Money.MoneyCalculator calculator = Money.calc();
		interventions.forEach(i -> calculator.add(i.getTotalAmount()));
		amount = calculator.toMoneyRoundedToCents();
	}

}
