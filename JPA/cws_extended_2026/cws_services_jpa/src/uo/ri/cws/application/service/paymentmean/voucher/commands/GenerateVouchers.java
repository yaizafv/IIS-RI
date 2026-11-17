package uo.ri.cws.application.service.paymentmean.voucher.commands;

import uo.ri.conf.Factories;
import uo.ri.cws.application.repository.ClientRepository;
import uo.ri.cws.application.repository.InvoiceRepository;
import uo.ri.cws.application.repository.PaymentMeanRepository;
import uo.ri.cws.application.util.command.Command;
import uo.ri.cws.domain.Associations;
import uo.ri.cws.domain.Client;
import uo.ri.cws.domain.Invoice;
import uo.ri.cws.domain.Voucher;
import uo.ri.cws.domain.types.Money;
import uo.ri.cws.domain.types.VoucherCode;
import uo.ri.util.exception.BusinessException;

/**
 * Generates the 20€ (work orders), 25€ (recommendations) and 30€ (invoices
 * over 500€) vouchers. Returns the number of vouchers generated of each type.
 */
public class GenerateVouchers implements Command<int[]> {

	private static final int WORK_ORDERS_PER_VOUCHER = 3;
	private static final int RECOMMENDATIONS_PER_VOUCHER = 3;
	private static final Money VOUCHER_20 = Money.of("20.00");
	private static final Money VOUCHER_25 = Money.of("25.00");
	private static final Money VOUCHER_30 = Money.of("30.00");
	private static final String WORK_ORDERS_DESCRIPTION = "Por tres órdenes de trabajo";
	private static final String RECOMMENDATIONS_DESCRIPTION = "Por recomendación";
	private static final String INVOICE_DESCRIPTION = "Por factura de más de 500";

	private ClientRepository clientRepo = Factories.repository.forClient();
	private InvoiceRepository invoiceRepo = Factories.repository.forInvoice();
	private PaymentMeanRepository pmRepo = Factories.repository.forPaymentMean();

	@Override
	public int[] execute() throws BusinessException {
		return new int[] {
				generateForWorkOrders(),
				generateForRecommendations(),
				generateForInvoices()
			};
	}

	private int generateForWorkOrders() {
		int generated = 0;
		for (Client client : clientRepo.findWithWorkOrdersUnusedForVoucher(WORK_ORDERS_PER_VOUCHER)) {
			while (client.isEligibleForVoucher20()) {
				client.markThreeWorkOrdersAsUsedForVoucher20();
				addVoucher(client, WORK_ORDERS_DESCRIPTION, VOUCHER_20);
				generated++;
			}
		}
		return generated;
	}

	private int generateForRecommendations() {
		int generated = 0;
		for (Client client : clientRepo.findWithRecommendationsUnusedForVoucher(RECOMMENDATIONS_PER_VOUCHER)) {
			while (client.isEligibleForVoucher25()) {
				client.markThreeRecommendationsAsUsedForVoucher25();
				addVoucher(client, RECOMMENDATIONS_DESCRIPTION, VOUCHER_25);
				generated++;
			}
		}
		return generated;
	}

	private int generateForInvoices() {
		int generated = 0;
		for (Invoice invoice : invoiceRepo.findOverAmountUnusedForVoucher(Invoice.VOUCHER_30_MIN_TOTAL)) {
			if (invoice.canGenerateVoucher30()) {
				invoice.markAsUsedForVoucher();
				addVoucher(invoice.getClient(), INVOICE_DESCRIPTION, VOUCHER_30);
				generated++;
			}
		}
		return generated;
	}

	private void addVoucher(Client client, String description, Money amount) {
		Voucher voucher = new Voucher(VoucherCode.generate(), description, amount);
		Associations.Holds.link(client, voucher);
		pmRepo.add(voucher);
	}

}
