package uo.ri.cws.application.ui.util;

import java.util.List;

import uo.ri.cws.application.service.client.ClientCrudService.ClientDto;
import uo.ri.cws.application.service.invoice.InvoicingService;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoicingWorkOrderDto;
import uo.ri.cws.application.service.invoice.InvoicingService.PaymentMeanDto;
import uo.ri.cws.application.service.mechanic.MechanicCrudService.MechanicDto;
import uo.ri.cws.application.service.vehicletype.VehicleTypeCrudService.VehicleTypeDto;
import uo.ri.cws.application.service.workorder.WorkOrderCrudService.WorkOrderDto;
import uo.ri.util.console.Console;

public class Printer {

    public static void printClient(ClientDto client) {
        Console.printf("\t%s %-10.10s %-15.15s %-25.25s %-20.20s %-15.15s %-15.15s %-10.10s %-15.15s %-25.25s\n",
            client.id, client.nif, client.name, client.surname, client.addressStreet, client.addressCity,
            client.addressCountry, client.addressZipcode, client.phone, client.email);
    }

    public static void printInvoice(InvoiceDto invoice) {

        Console.printf("Invoice #: %d\n", invoice.number);
        Console.printf("\tDate: %1$td/%1$tm/%1$tY\n", invoice.date);
        Console.printf("\tTotal: %.2f €\n", invoice.subtotal);
        Console.printf("\tTax amount: %s%n €\n", invoice.vatAmount);
        Console.printf("\tTotal, tax inc.: %s €\n", invoice.total);
        Console.printf("\tState: %s\n", invoice.state);
    }

    public static void printPaymentMeans(List<PaymentMeanDto> medios) {
        Console.println();
        Console.println("Available payment means");

        Console.printf("\t%s \t%-15.15s \t%s \t%s\n", "Id", "Type", "Accumulated", "Details");
        for (PaymentMeanDto medio : medios) {
            String details = "";
            if (medio instanceof InvoicingService.VoucherDto) {
                InvoicingService.VoucherDto voucher = (InvoicingService.VoucherDto) medio;
                details = "available " + voucher.availableAmount;
            } else if (medio instanceof InvoicingService.CardDto) {
                InvoicingService.CardDto card = (InvoicingService.CardDto) medio;
                details = "expiration " + card.cardExpiration;
            }
            Console.printf("\t%s \t%-15.15s \t%s \t%s\n", medio.id, medio.getClass().getSimpleName(), medio.accumulated,
                details);
        }
    }

    public static void printWorkOrder(WorkOrderDto rep) {

        Console.printf("\t%s \t%-40.40s \t%td/%<tm/%<tY \t%-12.12s \t%.2f\n", rep.id, rep.description, rep.date,
            rep.state, rep.amount);
    }

    public static void printWorkOrder(InvoicingWorkOrderDto rep) {

        Console.printf("\t%s \t%-40.40s \t%td/%<tm/%<tY \t%-12.12s \t%.2f\n", rep.id, rep.description, rep.date,
            rep.state, rep.amount);
    }

    public static void printMechanic(MechanicDto m) {

        Console.printf("\t%s %-10.10s %-15.15s %-25.25s\n", m.id, m.nif, m.name, m.surname);
    }

    public static void printVehicleType(VehicleTypeDto vt) {

        Console.printf("\t%s %-10.10s %5.2f €/hour %d\n", vt.id, vt.name, vt.pricePerHour, vt.minTrainigHours);
    }

    public static void printWorkOrderDetail(WorkOrderDto wo) {

        Console.printf("%s for vehicle %s\n\t%-25.25s\n\t%tm/%<td/%<tY\n\t%s\n", wo.id, wo.vehicleId, wo.description,
            wo.date, wo.state);
    }

}