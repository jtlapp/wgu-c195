package reports;

import javafx.scene.control.TableView;
import support.AppContext;
import database.DatabaseException;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Base class for reports shown on the reports tab of the main window. Each
 * report has a title and an associated table. Subclasses encapsulate reports
 * so that ReportsController need only ask a report for its title and a
 * constructed and populated table for display.
 * @author Joseph T. Lapp
 * @param <I> Class representing a row of the report table
 */

public abstract class Report<I> {

    /**
     * String to place in left-most column cells that would otherwise
     * duplicate the cell above it in that column.
     */
    protected final static String DUPLICATE_CELL_VALUE = "    \"";

    /**
     * Returns the name of the report
     * @return Name of the report
     */
    public abstract String getName();

    /**
     * Constructs the report as a populated instance of TableView.
     * @param appContext Global application context
     * @return The constructed, populated table serving as the report
     * @throws DatabaseException on database error
     */
    public abstract TableView<?> createTableView(AppContext appContext)
            throws DatabaseException;

    /**
     * Utility by which reports may give a table more of an appearance of
     * being organized into groups. The method receives an ordered list of
     * items and scans it for sequential duplicates, replacing each
     * duplicate after the first with a single set of quotation marks.
     * @param items Sorted list of items in which to abbreviate duplicates
     * @param getValue Function for reading values of an item
     * @param setValue Function for overwriting duplicate values of an item
     */
    protected void abbreviateDuplicates(List<I> items,
                                        Function<I,String> getValue,
                                        BiConsumer<I,String> setValue) {
        String previousValue = null;
        for (I item : items) {
            String value = getValue.apply(item);
            if (value.equals(previousValue)) {
                setValue.accept(item, DUPLICATE_CELL_VALUE);
            }
            previousValue = value;
        }
    }
}
