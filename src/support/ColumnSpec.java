package support;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * POJO for generically specifying a TableView column. Used by
 * SceneHelper.initializeTable() to add columns to a table from a
 * succinct ColumnSpec array specification for those columns.
 * @author Joseph T. Lapp
 * @see support.SceneHelper#initializeTable 
 * @param <I> Class representing the items to which table rows correspond
 */

public class ColumnSpec<I> {

    /**
     * Control representing the column being specified
     */
    public TableColumn<I, ?> column;

    /**
     * Name of the property of the item (I) to place in the column
     * JavaFX expects this property to have an associated getter.
     */
    public String propertyName;

    /**
     * Initial width to assign to the column
     */
    public int initialWidth;

    /**
     * Constructs a specification for a TableView column
     * @param column Control representing the column being specified
     * @param propertyName Name of the property of the item (I) to place in the column
     * @param initialWidth Initial width to assign to the column
     */
    public ColumnSpec(TableColumn<I, ?> column, String propertyName, int initialWidth) {
        this.column = column;
        this.propertyName = propertyName;
        this.initialWidth = initialWidth;
    }
}
