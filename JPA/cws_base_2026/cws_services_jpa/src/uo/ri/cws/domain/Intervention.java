package uo.ri.cws.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Transient;
import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.cws.domain.types.Money;
import uo.ri.util.assertion.ArgumentChecks;

/**
 * Represents an intervention performed by a mechanic on a work order. Internal
 * aggregate class of WorkOrder. Only WorkOrder can create Interventions.
 */
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "WORKORDER_ID", "MECHANIC_ID", "DATE" }) })
public class Intervention extends BaseEntity {
	// Natural attributes
	private LocalDateTime date;
	private int minutes;

	// Accidental attributes
	@ManyToOne
	private WorkOrder workOrder;
	@ManyToOne
	private Mechanic mechanic;
	@OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Substitution> substitutions = new HashSet<>();

	// ---------------------------- Constructors -----------------------------
	/**
	 * All these constructors are package-private Only WorkOrder (aggregate root)
	 * can create Interventions
	 */
	public Intervention() {
	}

	/* package */ Intervention(Mechanic mechanic, WorkOrder workOrder, int minutes) {
		this(mechanic, workOrder, LocalDateTime.now(), minutes);
	}

	/* package */ Intervention(Mechanic mechanic, WorkOrder workOrder, LocalDateTime date, int minutes) {
		this(mechanic, workOrder, date, minutes, Map.of());
	}

	/**
	 * Creates a new Intervention with the given parameters.
	 * 
	 * @param mechanic      The mechanic performing the intervention.
	 * @param workOrder     The work order associated with the intervention.
	 * @param date          The date and time of the intervention.
	 * @param minutes       The duration of the intervention in minutes.
	 * @param substitutions A map of spare parts and their quantities used in the
	 *                      intervention.
	 */
	/* package */ Intervention(Mechanic mechanic, WorkOrder workOrder, LocalDateTime date, int minutes,
			Map<SparePart, Integer> substitutions) {

		// Validate input parameters
		// Truncate the date to milliseconds to avoid precision issues
		// links the intervention with the work order and mechanic (atomiclink)
		// For each substitution in the map, add it to the intervention
		ArgumentChecks.isNotNull(mechanic, "Mechanic cannot be null");
		ArgumentChecks.isNotNull(workOrder, "Work order cannot be null");
		ArgumentChecks.isNotNull(date, "Date cannot be null");
		ArgumentChecks.isTrue(minutes >= 0, "Minutes cannot be negative");
		validateSubstitutions(substitutions);

		this.date = date.truncatedTo(ChronoUnit.MILLIS);
		this.minutes = minutes;
		Associations.RecordsIntervenes.atomicLink(workOrder, this, mechanic);

		substitutions.forEach(this::addSubstitution);
	}

	// ---------------------------- Association helpers
	// -----------------------------
	void _setMechanic(Mechanic mechanic) {
		this.mechanic = mechanic;
	}

	void _setWorkOrder(WorkOrder workOrder) {
		this.workOrder = workOrder;
	}

	public Set<Substitution> getSubstitutions() {
		return new HashSet<>(substitutions);
	}

	Set<Substitution> _getSubstitutions() {
		return substitutions;
	}

	// ---------------------------- Business methods -----------------------------

	/* package */ void removeSubstitutions() {
		new HashSet<>(substitutions).forEach(Associations.PerformsSubstitutes::atomicUnlink);
	}

	/**
	 * Calculates the total amount of the intervention, including labor and spare
	 * parts. Not an accounting boundary: the amount keeps full precision (rounded
	 * on work order approval).
	 * 
	 * @return The exact total amount of the intervention as a Money object.
	 */
	public Money getTotalAmount() {
		return Money.calc().add(calculateLaborAmount()).add(calculateSparesAmount()).toMoneyExact();
	}

	private Money calculateLaborAmount() {
		Money pricePerHour = workOrder.getPricePerHour();

		return Money.calc(pricePerHour).times(minutes).div(60) // mins/hour
				.toMoneyExact();
	}

	private Money calculateSparesAmount() {
		Money.MoneyCalculator calculator = Money.calc();
		substitutions.forEach(s -> calculator.add(s.getTotalAmount()));
		return calculator.toMoneyExact();
	}

	private void addSubstitution(SparePart sparePart, int quantity) {
		new Substitution(sparePart, this, quantity);
	}

	/**
	 * Validates the whole map before linking anything, so the intervention is
	 * created atomically.
	 */
	private static void validateSubstitutions(Map<SparePart, Integer> substitutions) {
		ArgumentChecks.isNotNull(substitutions, "Substitutions map cannot be null");
		substitutions.forEach((sparePart, quantity) -> {
			ArgumentChecks.isNotNull(sparePart, "Spare part cannot be null");
			ArgumentChecks.isNotNull(quantity, "Quantity cannot be null");
			ArgumentChecks.isTrue(quantity > 0, "Quantity must be greater than zero");
		});
	}

	// ---------------------------- Getters -----------------------------

	public LocalDateTime getDate() {
		return date;
	}

	public int getMinutes() {
		return minutes;
	}

	public WorkOrder getWorkOrder() {
		return workOrder;
	}

	public Mechanic getMechanic() {
		return mechanic;
	}

	// ---------------------------- Object methods -----------------------------

	@Override
	public int hashCode() {
		return Objects.hash(date, mechanic, workOrder);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Intervention other = (Intervention) obj;
		return Objects.equals(date, other.date) && Objects.equals(mechanic, other.mechanic)
				&& Objects.equals(workOrder, other.workOrder);
	}

	@Override
	public String toString() {
		return "Intervention [date=" + date + ", minutes=" + minutes + ", workOrder=" + workOrder + ", mechanic="
				+ mechanic + "]";
	}

}
