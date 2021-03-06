import org.sqlite.SQLiteDataSource;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Vector;

public class DataBase implements AutoCloseable {
    private Connection con = null;
    private final JComponent parent;

    private final String dbName;
    private ResultSet rs;

    protected final String BASIC_QUERY = "SELECT * FROM %s;";
    private final String URL;


    public DataBase(File file, JSplitPane parentComponent){
       this.URL = "jdbc:sqlite:"+file.getAbsolutePath();
       this.dbName = file.getName();
       this.parent = parentComponent;
       connect();
    }

    private void connect(){
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(URL);
        try {
            con = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public ArrayList<String> getTables(){
        String query = "SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%';";
        try {
            Statement st = con.createStatement();
            rs = st.executeQuery(query);
            ArrayList<String> tables = new ArrayList<>();
            while (rs.next()) tables.add(rs.getString(1));
            return tables;
        } catch (SQLException ex){
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public TableModel executeQuery(String query){
        try{
            // connection and statement
            Statement st = con.createStatement();
            if (query.toLowerCase().contains("select")){
                rs = st.executeQuery(query);
            } else {
                int success = st.executeUpdate(query);
                if (success == 0){
                    JOptionPane.showMessageDialog(parent, "Update Error", "SQL Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            }

            ResultSetMetaData meta = rs.getMetaData();

            // names of columns
            Vector<String> columnNames = new Vector<>();
            int columnCount = meta.getColumnCount();
            for (int column = 1; column <= columnCount; column++) {
                columnNames.add(meta.getColumnName(column));
            }

            // data of the table
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> vector = new Vector<>();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    vector.add(rs.getObject(columnIndex));
                }
                data.add(vector);
            }
            return new DefaultTableModel(data, columnNames){
                @Override
                public boolean isCellEditable(int row, int column){
                    return false;
                }
            }; 

        } catch (SQLException ex){
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

    }

    @Override
    public String toString() {
        return dbName;
    }

    @Override
    public void close() throws Exception {
        con.close();
    }

}
