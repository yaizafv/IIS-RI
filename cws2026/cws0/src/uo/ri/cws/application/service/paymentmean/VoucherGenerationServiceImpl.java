package uo.ri.cws.application.service.paymentmean;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.cws.application.service.paymentmean.VoucherGenerationService.VoucherSummaryDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

public class VoucherGenerationServiceImpl implements VoucherGenerationService {

    private final CommandExecutor executor = new CommandExecutor();

    @Override
    public int[] generateVouchers() throws BusinessException {
        return execute(() -> {
            Connection connection = Jdbc.getCurrentConnection();
            int workOrderVouchers = generateWorkOrderVouchers(connection);
            int recommendationVouchers = generateRecommendationVouchers(connection);
            int invoiceVouchers = generateInvoiceVouchers(connection);
            return new int[] { workOrderVouchers, recommendationVouchers, invoiceVouchers };
        });
    }

    @Override
    public List<VoucherDto> findVouchersByClientNif(String nif) throws BusinessException {
        ArgumentChecks.isNotBlank(nif, "El NIF del cliente no puede estar vacío");
        return execute(() -> {
            List<VoucherDto> vouchers = new ArrayList<>();
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    Queries.getSQLSentence("TVOUCHERS_FIND_BY_CLIENT_NIF"))) {
                pst.setString(1, nif);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        VoucherDto dto = new VoucherDto();
                        dto.id = rs.getString("id");
                        dto.version = rs.getLong("version");
                        dto.clientId = rs.getString("client_id");
                        dto.accumulatedAmount = rs.getBigDecimal("accumulated");
                        dto.code = rs.getString("code");
                        dto.description = rs.getString("description");
                        dto.availableAmount = rs.getBigDecimal("available");
                        vouchers.add(dto);
                    }
                }
            }
            return vouchers;
        });
    }

    @Override
    public List<VoucherSummaryDto> getVoucherSummary() throws BusinessException {
        return execute(() -> {
            List<VoucherSummaryDto> summaries = new ArrayList<>();
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    Queries.getSQLSentence("TVOUCHERS_SUMMARY"));
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    summaries.add(new VoucherSummaryDto(
                            rs.getString("nif"),
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getLong("issuedQty"),
                            rs.getBigDecimal("totalAmount"),
                            rs.getBigDecimal("availableAmount"),
                            rs.getBigDecimal("consumedAmount")));
                }
            }
            return summaries;
        });
    }

    private static int generateWorkOrderVouchers(Connection connection) throws SQLException {
        Map<String, List<String>> workOrdersByClient = new LinkedHashMap<>();
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_WORKORDERS_ELIGIBLE"));
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                workOrdersByClient.computeIfAbsent(rs.getString("client_id"),
                        ignored -> new ArrayList<>()).add(rs.getString("workorder_id"));
            }
        }

        int generated = 0;
        for (Map.Entry<String, List<String>> entry : workOrdersByClient.entrySet()) {
            List<String> ids = entry.getValue();
            int count = ids.size() / 3;
            for (int i = 0; i < count; i++) {
                List<String> group = ids.subList(i * 3, i * 3 + 3);
                markUsed(connection, "TWORKORDERS_MARK_USED_FOR_VOUCHER", group);
                createVoucher(connection, entry.getKey(), new BigDecimal("20.00"),
                        "Por tres órdenes de trabajo");
                generated++;
            }
        }
        return generated;
    }

    private static int generateRecommendationVouchers(Connection connection)
            throws SQLException {
        Map<String, List<String>> recommendationsByClient = new LinkedHashMap<>();
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_RECOMMENDATIONS_ELIGIBLE"));
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                recommendationsByClient.computeIfAbsent(rs.getString("recommender_id"),
                        ignored -> new ArrayList<>()).add(rs.getString("id"));
            }
        }

        int generated = 0;
        for (Map.Entry<String, List<String>> entry : recommendationsByClient.entrySet()) {
            List<String> ids = entry.getValue();
            int count = ids.size() / 3;
            for (int i = 0; i < count; i++) {
                markUsed(connection, "TRECOMMENDATIONS_MARK_USED_FOR_VOUCHER",
                        ids.subList(i * 3, i * 3 + 3));
                createVoucher(connection, entry.getKey(), new BigDecimal("25.00"),
                        "Por recomendación");
                generated++;
            }
        }
        return generated;
    }

    private static int generateInvoiceVouchers(Connection connection) throws SQLException {
        List<String[]> invoices = new ArrayList<>();
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_INVOICES_ELIGIBLE"));
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                invoices.add(new String[] { rs.getString("id"), rs.getString("client_id") });
            }
        }

        for (String[] invoice : invoices) {
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TINVOICES_MARK_USED_FOR_VOUCHER"))) {
                pst.setString(1, invoice[0]);
                pst.executeUpdate();
            }
            createVoucher(connection, invoice[1], new BigDecimal("30.00"),
                    "Por factura de más de 500");
        }
        return invoices.size();
    }

    private static void markUsed(Connection connection, String queryKey, List<String> ids)
            throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence(queryKey))) {
            for (String id : ids) {
                pst.setString(1, id);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }

    private static void createVoucher(Connection connection, String clientId,
            BigDecimal amount, String description) throws SQLException {
        String id = UUID.randomUUID().toString();
        Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_ADD_GENERATED"))) {
            pst.setString(1, id);
            pst.setTimestamp(2, now);
            pst.setTimestamp(3, now);
            pst.setLong(4, 1);
            pst.setString(5, "ENABLED");
            pst.setBigDecimal(6, BigDecimal.ZERO);
            pst.setString(7, clientId);
            pst.executeUpdate();
        }
        try (PreparedStatement pst = connection.prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_ADD"))) {
            pst.setString(1, id);
            pst.setString(2, description);
            pst.setBigDecimal(3, amount);
            pst.setString(4, "V-" + UUID.randomUUID());
            pst.executeUpdate();
        }
    }

    private <T> T execute(SqlCommand<T> command) throws BusinessException {
        return executor.execute(() -> {
            try {
                return command.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @FunctionalInterface
    private interface SqlCommand<T> {
        T execute() throws SQLException, BusinessException;
    }
}
