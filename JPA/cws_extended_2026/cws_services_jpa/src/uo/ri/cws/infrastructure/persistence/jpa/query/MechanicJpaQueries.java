package uo.ri.cws.infrastructure.persistence.jpa.query;

import uo.ri.cws.application.query.MechanicQueries;
import uo.ri.cws.infrastructure.persistence.jpa.util.Jpa;

public class MechanicJpaQueries implements MechanicQueries {

	@Override
	public boolean hasInterventions(String mechanicId) {
		return 0 < Jpa.getManager()
				.createNamedQuery("Mechanic.countInterventions", Long.class)
				.setParameter(1, mechanicId)
				.getSingleResult();
	}

}
