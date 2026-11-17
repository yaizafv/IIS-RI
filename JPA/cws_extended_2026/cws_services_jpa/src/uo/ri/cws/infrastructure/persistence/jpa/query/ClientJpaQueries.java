package uo.ri.cws.infrastructure.persistence.jpa.query;

import uo.ri.cws.application.query.ClientQueries;
import uo.ri.cws.infrastructure.persistence.jpa.util.Jpa;

public class ClientJpaQueries implements ClientQueries {

	@Override
	public boolean hasPaidInvoices(String clientId) {
		return 0 < Jpa.getManager()
				.createNamedQuery("Client.countPaidInvoices", Long.class)
				.setParameter(1, clientId)
				.getSingleResult();
	}

}
