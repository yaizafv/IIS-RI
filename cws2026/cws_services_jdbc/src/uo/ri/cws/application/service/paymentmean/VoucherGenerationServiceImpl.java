package uo.ri.cws.application.service.paymentmean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.EligibleRecord;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.SummaryRecord;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherSummaryDto;
import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;

public class VoucherGenerationServiceImpl implements VoucherGenerationService {

    private final CommandExecutor executor = new CommandExecutor();

    @Override
    public int[] generateVouchers() throws BusinessException {
        return executor.execute(() -> {
            VoucherGateway gateway = Factories.persistence.forVoucher();
            int workOrders = generateWorkOrderVouchers(gateway);
            int recommendations = generateRecommendationVouchers(gateway);
            int invoices = generateInvoiceVouchers(gateway);
            return new int[] { workOrders, recommendations, invoices };
        });
    }

    @Override
    public List<VoucherDto> findVouchersByClientNif(String nif) throws BusinessException {
        ArgumentChecks.isNotBlank(nif, "El NIF del cliente no puede estar vacío");
        return executor.execute(() -> {
            List<VoucherDto> result = new ArrayList<>();
            for (VoucherGateway.VoucherRecord record :
                    Factories.persistence.forVoucher().findByClientNif(nif)) {
                VoucherDto dto = new VoucherDto();
                dto.id = record.id;
                dto.version = record.version;
                dto.clientId = record.clientId;
                dto.accumulatedAmount = record.accumulated;
                dto.code = record.code;
                dto.description = record.description;
                dto.availableAmount = record.available;
                result.add(dto);
            }
            return result;
        });
    }

    @Override
    public List<VoucherSummaryDto> getVoucherSummary() throws BusinessException {
        return executor.execute(() -> {
            List<VoucherSummaryDto> result = new ArrayList<>();
            for (SummaryRecord summary : Factories.persistence.forVoucher().findSummary()) {
                result.add(new VoucherSummaryDto(summary.nif(), summary.name(),
                        summary.surname(), summary.issuedQty(), summary.totalAmount(),
                        summary.availableAmount(), summary.consumedAmount()));
            }
            return result;
        });
    }

    private static int generateWorkOrderVouchers(VoucherGateway gateway) {
        Map<String, List<String>> byClient = group(gateway.findEligibleWorkOrders());
        int generated = 0;
        for (Map.Entry<String, List<String>> entry : byClient.entrySet()) {
            List<String> ids = entry.getValue();
            for (int offset = 0; offset + 2 < ids.size(); offset += 3) {
                for (String id : ids.subList(offset, offset + 3)) {
                    gateway.markWorkOrderUsed(id);
                }
                gateway.addGenerated(entry.getKey(), new BigDecimal("20.00"),
                        "Por tres órdenes de trabajo");
                generated++;
            }
        }
        return generated;
    }

    private static int generateRecommendationVouchers(VoucherGateway gateway) {
        Map<String, List<String>> byClient = group(gateway.findEligibleRecommendations());
        int generated = 0;
        for (Map.Entry<String, List<String>> entry : byClient.entrySet()) {
            List<String> ids = entry.getValue();
            for (int offset = 0; offset + 2 < ids.size(); offset += 3) {
                for (String id : ids.subList(offset, offset + 3)) {
                    gateway.markRecommendationUsed(id);
                }
                gateway.addGenerated(entry.getKey(), new BigDecimal("25.00"),
                        "Por recomendación");
                generated++;
            }
        }
        return generated;
    }

    private static int generateInvoiceVouchers(VoucherGateway gateway) {
        int generated = 0;
        for (EligibleRecord invoice : gateway.findEligibleInvoices()) {
            gateway.markInvoiceUsed(invoice.itemId());
            gateway.addGenerated(invoice.clientId(), new BigDecimal("30.00"),
                    "Por factura de más de 500");
            generated++;
        }
        return generated;
    }

    private static Map<String, List<String>> group(List<EligibleRecord> records) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (EligibleRecord record : records) {
            result.computeIfAbsent(record.clientId(), ignored -> new ArrayList<>())
                    .add(record.itemId());
        }
        return result;
    }
}
