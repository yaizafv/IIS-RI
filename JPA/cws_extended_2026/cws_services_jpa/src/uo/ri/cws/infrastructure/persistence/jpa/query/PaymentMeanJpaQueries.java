package uo.ri.cws.infrastructure.persistence.jpa.query;

import java.math.BigDecimal;
import java.util.List;

import uo.ri.cws.application.query.PaymentMeanQueries;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherSummaryDto;
import uo.ri.cws.infrastructure.persistence.jpa.util.Jpa;

public class PaymentMeanJpaQueries implements PaymentMeanQueries {

	@Override
	public boolean hasCharges(String paymentMeanId) {
		return 0 < Jpa.getManager()
				.createNamedQuery("PaymentMean.countCharges", Long.class)
				.setParameter(1, paymentMeanId)
				.getSingleResult();
	}

	@Override
	public List<VoucherSummaryDto> getVouchersSummary() {
		return Jpa.getManager()
				.createNamedQuery("Voucher.getSummary", Object[].class)
				.getResultStream()
				.map(PaymentMeanJpaQueries::toSummaryDto)
				.toList();
	}

	/**
	 * Maps a row: nif, name, surname, issued, total, available, consumed
	 */
	private static VoucherSummaryDto toSummaryDto(Object[] row) {
		return new VoucherSummaryDto(
				(String) row[0],
				(String) row[1],
				(String) row[2],
				((Number) row[3]).longValue(),
				(BigDecimal) row[4],
				(BigDecimal) row[5],
				(BigDecimal) row[6]);
	}

}
