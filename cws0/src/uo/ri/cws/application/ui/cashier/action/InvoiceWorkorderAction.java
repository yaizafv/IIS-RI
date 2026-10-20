package uo.ri.cws.application.ui.cashier.action;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uo.ri.util.assertion.BusinessChecks;
import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.jdbc.Jdbc;
import uo.ri.util.menu.Action;

public class InvoiceWorkorderAction implements Action {

	private static final String TINVOICES_ADD = 
			"INSERT INTO TInvoices (id, version, number, date, state, subtotal, total, vatRate, vatAmount, "
			+ "createdAt, updatedAt, entityState) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
    private static final String TINVOICES_FINDNEXTNUMBER = 
            "SELECT MAX(number) FROM TInvoices";
    private static final String TWORKORDERS_FINDAMOUNT = 
            "SELECT total_amount FROM TWorkOrders WHERE id = ?";
    private static final String TWORKORDERS_FINDID = 
            "SELECT id FROM TWorkOrders WHERE id = ?";
    private static final String TWORKORDERS_FINDSTATE = 
            "SELECT state FROM TWorkOrders WHERE id = ?";
    private static final String TWORKORDERS_UPDATEINVOICEID = 
            "UPDATE TWorkOrders SET invoice_id = ? WHERE id = ?";
    private static final String TWORKORDERS_UPDATESTATE = 
            "UPDATE TWorkOrders SET state = 'INVOICED' WHERE id = ?";
    private static final String TWORKORDERS_UPDATEVERSION = 
            "update TWorkOrders set version=version+1 where id = ?";
    private static final String TWORKORDERS_UPDATE_TIME_VERSION = 
            "update TWorkOrders set updatedAt = ? where id = ?";
    
    @Override
    public void execute() throws BusinessException {
        List<String> workOrderIds = new ArrayList<>();

        // Ask the user the work order ids
        do {
            String id = Console.readString("Workorder id");
            workOrderIds.add(id);
        } while (moreWorkOrders());

        try (Connection ignored = Jdbc.createThreadConnection()) {

            BusinessChecks.isTrue(checkWorkOrdersExist(workOrderIds), "some workorder does not exist");
            BusinessChecks.isTrue(checkWorkOrdersApproved(workOrderIds), "some workorder is not approved yet");

            long numberInvoice = generateInvoiceNumber();
            LocalDate dateInvoice = LocalDate.now();
            BigDecimal subtotal = calculateTotalInvoice(workOrderIds); // vat not included
            double vatRate = vatPercentage(dateInvoice);
            BigDecimal vat = BigDecimal.valueOf(vatPercentage(dateInvoice));
    		BigDecimal vatAmount = subtotal.multiply(vat).divide(BigDecimal.valueOf(100));
    		BigDecimal total = subtotal.add(vatAmount);

            String idInvoice = createInvoice(numberInvoice,
                    dateInvoice,
                    subtotal,
                    total,
                    vatRate,
                    vatAmount);
            
            linkWorkordersToInvoice(idInvoice, workOrderIds);
            markWorkOrderAsInvoiced(workOrderIds);
            updateVersion(workOrderIds);
            updateTimeVersion(workOrderIds);
            displayInvoice(numberInvoice, dateInvoice, subtotal, vat, total);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateTimeVersion(List<String> workOrderIds) throws SQLException {
        Connection c = Jdbc.getCurrentConnection();

        try (PreparedStatement pst = c
                .prepareStatement(TWORKORDERS_UPDATE_TIME_VERSION)) {
            for (String workOrderID : workOrderIds) {
                pst.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                pst.setString(2, workOrderID);
                pst.executeUpdate();
            }
        }
		
	}

	private void updateVersion(List<String> workOrderIds) throws SQLException {
        Connection c = Jdbc.getCurrentConnection();

        try (PreparedStatement pst = c
                .prepareStatement(TWORKORDERS_UPDATEVERSION)) {
            for (String workOrderID : workOrderIds) {
                pst.setString(1, workOrderID);
                pst.executeUpdate();
            }
        }
    }

    private boolean moreWorkOrders() {
        return Console.readString("more work orders? (y/n) ")
                .equalsIgnoreCase("y");
    }

    /*
     * checks whether every work order exist
     */
    private boolean checkWorkOrdersExist(List<String> workOrderIDS)
            throws SQLException {

        Connection c = Jdbc.getCurrentConnection();

        try (PreparedStatement pst = c.prepareStatement(TWORKORDERS_FINDID)) {
            for (String workOrderID : workOrderIDS) {
                pst.setString(1, workOrderID);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next())
                        return false; // Si no encuentra la orden de trabajo
                }
            }
        }
        return true;
    }

    /*
     * checks whether every work order id is APPROVED
     */
    private boolean checkWorkOrdersApproved(List<String> workOrderIDs)
            throws SQLException {
        Connection connection = Jdbc.getCurrentConnection();

        try (PreparedStatement pst = connection
                .prepareStatement(TWORKORDERS_FINDSTATE)) {
            for (String id : workOrderIDs) {
                pst.setString(1, id);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("state");
                        if (!"APPROVED".equalsIgnoreCase(status)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /*
     * Generates next invoice number (not to be confused with the inner id)
     */
    private long generateInvoiceNumber() throws SQLException {
        Connection connection = Jdbc.getCurrentConnection();

        try (PreparedStatement pst = connection
                .prepareStatement(TINVOICES_FINDNEXTNUMBER)) {
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) + 1;
                }
            }
        }
        return 1L; // Si no hay facturas previas, empezamos desde 1
    }

    /*
     * Compute total amount of the invoice (as the total of individual work
     * orders' amount
     */
    private BigDecimal calculateTotalInvoice(List<String> workOrderIDs)
            throws SQLException {
        Connection connection = Jdbc.getCurrentConnection();

        BigDecimal total = BigDecimal.ZERO;
        try (PreparedStatement pst = connection
                .prepareStatement(TWORKORDERS_FINDAMOUNT)) {
            for (String id : workOrderIDs) {
                pst.setString(1, id);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        total = total.add(rs.getBigDecimal("total_amount"));
                    }
                }
            }
        }
        return total;
    }

    /*
     * returns vat percentage
     */
    private double vatPercentage(LocalDate d) {
        return LocalDate.parse("2012-07-01").isBefore(d) ? 21.0 : 18.0;

    }

    /*
     * Creates the invoice in the database; returns the id
     */
    private String createInvoice(long numberInvoice,
            LocalDate dateInvoice,
            BigDecimal subtotal,
            BigDecimal total,
            double vatRate,
            BigDecimal vatAmount) throws SQLException {
        Connection connection = Jdbc.getCurrentConnection();

        String idInvoice = UUID.randomUUID().toString();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        try (PreparedStatement pst = connection
                .prepareStatement(TINVOICES_ADD)) {
        	pst.setString(1, idInvoice);
            pst.setLong(2, 1L);
            pst.setLong(3, numberInvoice);
            pst.setDate(4, Date.valueOf(dateInvoice));
            pst.setString(5, "ISSUED");
            pst.setBigDecimal(6, subtotal);
            pst.setBigDecimal(7, total); // Round to 2 decimal places
            pst.setDouble(8, vatRate);
            pst.setBigDecimal(9, vatAmount);
            pst.setTimestamp(10, now);
            pst.setTimestamp(11, now);
            pst.setString(12, "ENABLED");
            pst.executeUpdate();
        }
        return idInvoice;
    }

    
    /*
     * Set the invoice number field in work order table to the invoice number
     * generated
     */
    private void linkWorkordersToInvoice(String invoiceId,
            List<String> workOrderIDs) throws SQLException {
        Connection connection = Jdbc.getCurrentConnection();

        try (PreparedStatement pst = connection
                .prepareStatement(TWORKORDERS_UPDATEINVOICEID)) {
            for (String id : workOrderIDs) {
                pst.setString(1, invoiceId);
                pst.setString(2, id);
                pst.executeUpdate();
            }
        }
    }

    /*
     * Sets status to INVOICED for every workorder
     */
    private void markWorkOrderAsInvoiced(List<String> ids) throws SQLException {
        Connection connection = Jdbc.getCurrentConnection();

        try (PreparedStatement pst = connection
                .prepareStatement(TWORKORDERS_UPDATESTATE)) {
            for (String id : ids) {
                pst.setString(1, id);
                pst.executeUpdate();
            }
        }
    }

    private void displayInvoice(long numberInvoice,
            LocalDate dateInvoice,
            BigDecimal amount,
            BigDecimal vat,
            BigDecimal total) {

        Console.printf("Invoice number: %d\n", numberInvoice);
        Console.printf("\tDate: %1$td/%1$tm/%1$tY\n", dateInvoice);
        Console.printf("\tAmount: %.2f €\n", amount);
        Console.printf("\tVAT: %.1f %% \n", vat);
        Console.printf("\tTotal (including VAT): %.2f €\n", total);
    }
}
