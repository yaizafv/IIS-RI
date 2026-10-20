package uo.ri.cws.application.ui.manager.mechanic.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.util.console.Console;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.jdbc.Jdbc;
import uo.ri.util.menu.Action;

public class ListAllMechanicsAction implements Action {

    private static final String TMECHANICS_FINDALL = "SELECT ID, NAME, "
            + "SURNAME, NIF, VERSION FROM TMECHANICS";

    @Override
    public void execute() throws BusinessException {

        Console.println("\nList of mechanics \n");

        // Process
        try (Connection c = Jdbc.createThreadConnection()) {
            try (PreparedStatement pst = c
                    .prepareStatement(TMECHANICS_FINDALL)) {
                try (ResultSet rs = pst.executeQuery();) {
                    while (rs.next()) {
                        Console.printf("\t%s %s %s %s %d\n",
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getString(4),
                                rs.getLong(5));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}