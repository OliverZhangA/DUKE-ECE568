package edu.duke.ece568.amazon;

import edu.duke.ece568.amazon.protos.WorldAmazon.*;
//import edu.duke.ece568.amazon.protos.AmazonUps.*;
//import JDBC
import java.sql.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


public class dbProcess {
    //private static final String URL = "jdbc:postgresql:postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USERNAME = "postgres";
    private static final String PASSWD = "passw0rd";

    // tables
    private static final String PRODUCT = "shopping_commodity";
    private static final String ORDER = "shopping_order";
    private static final String PACKAGE = "shopping_package_info";
    private static final String WAREHOUSE = "shopping_warehouse";

    public dbProcess(){}

    /*============ Query functions for backend to access DB ==============*/

    // public List<AInitWarehouse> initAmazonWarehouse() throws ClassNotFoundException, SQLException{
    //     List<AInitWarehouse> warehouses_list = new ArrayList<>();
        
    //     //loading the driver
    //     Class.forName("org.postgresql.Driver");
    //     Connection db = DriverManager.getConnection(URL, USERNAME, PASSWD);
    //     db.setAutoCommit(false);

    //     Statement W = db.createStatement();
    //     String sql_line = String.format("SELECT * FROM %s;", WAREHOUSE);
    //     ResultSet R = W.executeQuery(sql_line);

    //     while(R.next()){
    //         int warehouse_id = R.getInt("id");
    //         int x = R.getInt("pos_x");
    //         int y = R.getInt("pos_y");
    //         AInitWarehouse.Builder ainitwarehouse = AInitWarehouse.newBuilder();
    //         ainitwarehouse.setId(warehouse_id);
    //         ainitwarehouse.setX(x).setY(y);
    //         warehouses_list.add(ainitwarehouse.build());
    //     }

    //     //close sql and driver
    //     W.close();
    //     db.close();
    //     return warehouses_list;
    // }
    //access the warehouse table to get the warehouse to initialize AInitWarehouse
    public Map<Integer, AInitWarehouse> initAmazonWarehouse() throws ClassNotFoundException, SQLException{
        Map<Integer, AInitWarehouse> warehouses_list = new ConcurrentHashMap<>();
        
        //loading the driver
        Class.forName("org.postgresql.Driver");
        Connection db = DriverManager.getConnection(URL, USERNAME, PASSWD);
        db.setAutoCommit(false);

        Statement W = db.createStatement();
        String sql_line = String.format("SELECT * FROM %s;", WAREHOUSE);
        ResultSet R = W.executeQuery(sql_line);

        while(R.next()){
            int warehouse_id = R.getInt("id");
            int x = R.getInt("pos_x");
            int y = R.getInt("pos_y");
            AInitWarehouse.Builder ainitwarehouse = AInitWarehouse.newBuilder();
            ainitwarehouse.setId(warehouse_id);
            ainitwarehouse.setX(x).setY(y);
            warehouses_list.put(warehouse_id, ainitwarehouse.build());
        }

        //close sql and driver
        W.close();
        db.close();
        return warehouses_list;
    }

    //access the packageinfo table to construct the APurchaseMore
    public void construcBuyrqst(long package_id, APurchaseMore.Builder apurchasemore) throws ClassNotFoundException, SQLException{
        // APurchaseMore.Builder apurchasemore = APurchaseMore.newBuilder();
        int whnum = getwhnum(package_id);
        apurchasemore.setWhnum(whnum);
        
        //loading the driver
        Class.forName("org.postgresql.Driver");
        Connection db = DriverManager.getConnection(URL, USERNAME, PASSWD);
        db.setAutoCommit(false);

        Statement W = db.createStatement();
        String sql_line = String.format("SELECT shopping_commodity.id, shopping_commodity.commodity_desc, shopping_order.commodity_amt FROM %s, %s WHERE shopping_order.commodity_id = shopping_commodity.id AND shopping_order.package_info_id = %d;", PRODUCT, ORDER, package_id);
        ResultSet R = W.executeQuery(sql_line);

        while (R.next()){
            int product_id = R.getInt("id");
            String description = R.getString("commodity_desc");
            int count = R.getInt("commodity_amt");
            AProduct.Builder aproduct = AProduct.newBuilder();
            aproduct.setId(product_id).setDescription(description).setCount(count);
            apurchasemore.addThings(aproduct);
        }
        
        //close sql and driver
        W.close();
        db.close();
        //return apurchasemore.build();
    }

    //helper function for setting the whnum for APurchaseMore
    public int getwhnum(long package_id) throws ClassNotFoundException, SQLException{
        //set default is -1
        int whnum = -1;
        //loading the driver
        Class.forName("org.postgresql.Driver");
        Connection db = DriverManager.getConnection(URL, USERNAME, PASSWD);
        db.setAutoCommit(false);

        Statement W = db.createStatement();
        String sql_line = String.format("SELECT shopping_package_info.from_wh_id FROM %s WHERE id = %d;", PACKAGE, package_id);
        ResultSet R = W.executeQuery(sql_line);
        
        //set the whnum
        if (R.next()){
            whnum  = R.getInt("from_wh_id");
        }

        //close sql and driver
        W.close();
        db.close();
        return whnum;
    }

    //update the status in our database
    public void setStatusforDB(long package_id, String status) throws ClassNotFoundException, SQLException{
        Class.forName("org.postgresql.Driver");
        Connection db = DriverManager.getConnection(URL, USERNAME, PASSWD);
        db.setAutoCommit(false);

        Statement W = db.createStatement();
        System.out.println("updating the status of package <" + package_id + "> to" + status);
        String sql_line = String.format("UPDATE %s SET status = '%s' WHERE id = %d;", PACKAGE, status, package_id);
        W.executeUpdate(sql_line);
        db.commit();
        //close sql and driver
        W.close();
        db.close();
        
    }

    //access the package_info table to set the destination according to the package id
    public destination setDest(long package_id) throws ClassNotFoundException, SQLException{
        destination dest = new destination();
        
        Class.forName("org.postgresql.Driver");
        Connection db = DriverManager.getConnection(URL, USERNAME, PASSWD);
        db.setAutoCommit(false);
        Statement W = db.createStatement();
        String sql_line = String.format("SELECT dest_x, dest_y FROM %s WHERE id = %d;", PACKAGE, package_id);
        ResultSet R = W.executeQuery(sql_line);

        if (R.next()){
            dest.setX(R.getInt("dest_x"));
            dest.setY(R.getInt("dest_y"));
        }
        W.close();
        db.close();
        return dest;
    }

    //access the package_info to set the username
    public String InitAmazonAccount(long package_id) throws ClassNotFoundException, SQLException{
        String username = "";
        Class.forName("org.postgresql.Driver");
        Connection db = DriverManager.getConnection(URL, USERNAME, PASSWD);
        db.setAutoCommit(false);
        Statement W = db.createStatement();
        //String sql_line = String.format("SELECT shopping_package_info.owner_username FROM %s WHERE id = %d;", PACKAGE, package_id);
        String sql_line = String.format("SELECT auth_user.username FROM auth_user, %s WHERE shopping_package_info.id = %d AND auth_user.id = shopping_package_info.owner_id;", PACKAGE, package_id);
        ResultSet R = W.executeQuery(sql_line);

        if (R.next()){
            // username = R.getString("shopping_package_info.owner_username");
            username = R.getString("username");
        }
        W.close();
        db.close();
        return username;

    }

    // public static void main(String[] args) throws SQLException, ClassNotFoundException {
    //     dbProcess db = new dbProcess();
    //     System.out.println(db.initAmazonWarehouse());;
    //     System.out.println(db.InitAmazonAccount(36));
    // }
    
}
