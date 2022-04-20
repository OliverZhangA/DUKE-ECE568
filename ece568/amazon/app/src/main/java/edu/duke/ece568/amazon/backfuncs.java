package edu.duke.ece568.amazon;

import static edu.duke.ece568.amazon.interactions.sendMesgTo;
import static edu.duke.ece568.amazon.interactions.recvMesgFrom;
import edu.duke.ece568.amazon.dbProcess.*;

import edu.duke.ece568.amazon.protos.AmazonUps.*;
import edu.duke.ece568.amazon.protos.AmazonUps.Error;
import edu.duke.ece568.amazon.protos.WorldAmazon.*;

import java.io.IOException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;

public class backfuncs {
    private static final String WORLD_HOST = "vcm-26608.vm.duke.edu";
    private static final String UPS_HOST = "vcm-26608.vm.duke.edu";
    private static final int WORLD_PORT = 23456;
    private static final int UPS_PORT = 33333;
    private static final int FRONT_PORT = 7777;

    private static final int MAXTIME = 20000;

    //private List<AInitWarehouse> warehouses = new ArrayList<>();
    private Map<Integer, AInitWarehouse> warehouses;
    Socket toups;
    Socket toWorld;
    private final Map<Long, Package> package_list;
    private long seqnum;
    //private final ThreadPoolExecutor threadPool;
    private final Map<Long, Timer> rqst_list;

    //construct function
    public backfuncs() throws IOException, ClassNotFoundException, SQLException{
        // AInitWarehouse.Builder newWH = AInitWarehouse.newBuilder().setId(1).setX(5).setY(5);
        // warehouses.add(newWH.build());
        dbProcess database = new dbProcess();
        warehouses = database.initAmazonWarehouse();
        //print our result of warehouses initialization
        for (Map.Entry<Integer, AInitWarehouse> entry : warehouses.entrySet())
            System.out.println("Wh_Id = " + entry.getKey() +
                             ", Value = " + entry.getValue());
        package_list = new ConcurrentHashMap<>();
        seqnum = 0;
        rqst_list = new ConcurrentHashMap<>();
    }

    //DEALING WITH UPS responses!!


    //amazon connect to ups
    public void connect_ups() throws IOException{
        System.out.println("connecting to ups server");
        toups = new Socket(UPS_HOST, UPS_PORT);
        while(true){
            if(toups != null){
                U2AConnect.Builder connect = U2AConnect.newBuilder();
                recvMesgFrom(connect, toups.getInputStream());
                if(connect.hasWorldid()){
                    //connect to world
                    //if connect successfully
                    long world_id = connect.getWorldid();
                    System.out.println("worldid is: " + world_id);
                    A2UConnected.Builder connected = A2UConnected.newBuilder();
                    if(connect_world(world_id)){
                        System.out.println("connected to world yeah");
                        connected.setWorldid(world_id).setResult("connected!");
                        sendMesgTo(connected.build(), toups.getOutputStream());
                        break;
                    }
                    else{
                        String result_msg = String.format("error: Amazon fail to connect the World %d", world_id);
                        connected.setResult(result_msg);
                    }
                }
            }
        }
    }

    //amazon connect to world
    public boolean connect_world(long id) throws IOException{
        System.out.println("connecting to World simulator");
        toWorld = new Socket(WORLD_HOST, WORLD_PORT);
        System.out.println("world socket");

        AConnect.Builder connect = AConnect.newBuilder();
        //connect.setWorldid(id).setIsAmazon(true).addAllInitwh(warehouses);
        connect.setWorldid(id).setIsAmazon(true);
        for(Map.Entry<Integer, AInitWarehouse> entry : warehouses.entrySet()){
            connect.addInitwh(entry.getValue());
        }
        sendMesgTo(connect.build(), toWorld.getOutputStream());

        AConnected.Builder connected = AConnected.newBuilder();
        recvMesgFrom(connected, toWorld.getInputStream());
        System.out.println("result from world is:" + connected.getResult());
        return connected.getResult().equals("connected!");
    }

    //thread for comm with ups
    public void init_upsthread() throws IOException{
        while(!Thread.currentThread().isInterrupted()) {
            if(toups!=null){
                UPSCommands.Builder recvUps = UPSCommands.newBuilder();
                recvMesgFrom(recvUps, toups.getInputStream());
                Thread upsthread = new Thread(() -> {
                    try {
                        handle_ups(recvUps);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                });
                upsthread.start();
            }
        }
    }

    //thread for comm with the world
    public void init_worldthread() throws IOException{
        while(!Thread.currentThread().isInterrupted()) {
            if(toWorld!=null){
                AResponses.Builder aresponses = AResponses.newBuilder();
                recvMesgFrom(aresponses, toWorld.getInputStream());
                Thread worldthread = new Thread(() -> {
                    try {
                        handle_world(aresponses);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                });
                worldthread.start();
            }
        }
    }

    //handle request from Ups that "truck arrived"
    void truckArrived(U2ATruckArrived.Builder upstruckarrived) throws IOException, ClassNotFoundException, SQLException{
        //to be modified: update the truck arrived protocol to have package_id!!
        for(long package_id : upstruckarrived.getShipidList()){
            if(package_list.containsKey(package_id)){
                Package pkg = package_list.get(package_id);
                System.out.println("UPS truck arrived");
                pkg.setTruckid(upstruckarrived.getTruckid());
                //check if amazon packed or not
                if(pkg.getPackageStatus().equals("packed")){
                    //start loading
                    worldPutOnTruck(pkg);
                }
            }
            else {
                //can not find the package according to id
                System.out.println("package does not exists!");
            }
        }
    }

    //handle request from Ups that "package delivering"
    void packageDelivering(U2ADelivering.Builder upsPkgDelivering) throws IOException, ClassNotFoundException, SQLException {
        for(long id : upsPkgDelivering.getShipidList()){
            if(package_list.containsKey(id)){
                package_list.get(id).setStatus("delivering");
            } else {
                //can not find the package according to id
                System.out.println("package update to delivering does not exists!");
            }
        }
    }

    //handle request from Ups that "package delivering"
    void packageDelivered(U2ADelivered.Builder upsPkgDelivered) throws IOException, ClassNotFoundException, SQLException {
        //到底是单独还是重复
        for(long id : upsPkgDelivered.getShipidList()){
            if(package_list.containsKey(id)){
                package_list.get(id).setStatus("delivered");
                package_list.remove(id);
            } else {
                //can not find the package according to id
                System.out.println("package update to delivering does not exists!");
            }
        }
        // long package_id = upsPkgDelivered.getShipid();
    }

    //handle communication with Ups
    public void handle_ups(UPSCommands.Builder recvUps) throws IOException, ClassNotFoundException, SQLException{
        // UPSCommands.Builder recvUps = UPSCommands.newBuilder();
        // recvMesgFrom(recvUps, toups.getInputStream());
        ackToUps(recvUps);
        for(U2ATruckArrived x : recvUps.getArrivedList()){
            truckArrived(x.toBuilder());
        }
        for(U2ADelivering x : recvUps.getDeliveringList()){
            packageDelivering(x.toBuilder());
        }
        for(U2ADelivered x : recvUps.getDeliveredList()){
            packageDelivered(x.toBuilder());
        }
        // for(U2AShipStatus x : recvUps.getStatusList()){
        //     //response to query status
        // }
        // for(U2AChangeAddress x : recvUps.getAddressList()){
        //     //change the address
               //如果我们前端需要显示地址就需要做操作
        // }
        for(edu.duke.ece568.amazon.protos.AmazonUps.Error x : recvUps.getErrorList()){
            //response to query status
            if(x.getInfo() != null){
                System.err.println(x.getInfo());
            }
        }
        if(recvUps.hasFinish()){
            System.out.println("UPS close the connection, finish!");
        }
    }

    //send acks back to Ups
    void ackToUps(UPSCommands.Builder recvUps) throws IOException{
        for(U2ATruckArrived x : recvUps.getArrivedList()){
            recvUps.addAcks(x.getSeqnum());
        }
        for(U2ADelivering x : recvUps.getDeliveringList()){
            recvUps.addAcks(x.getSeqnum());
        }
        for(U2ADelivered x : recvUps.getDeliveredList()){
            recvUps.addAcks(x.getSeqnum());
        }
        for(U2AShipStatus x : recvUps.getStatusList()){
            recvUps.addAcks(x.getSeqnum());
        }
        // for(U2AChangeAddress x : recvUps.getAddressList()){
        //     recvUps.addAcks(x.getSeqnum());
        // }
        for(Error x : recvUps.getErrorList()){
            recvUps.addAcks(x.getSeqnum());
        }
        System.out.println("sending acks back to Ups");
        sendMesgTo(recvUps.build(), toups.getOutputStream());
    }


    //DEALING WITH WORLD responses!!
    //to do:start handle_world
    public void handle_world(AResponses.Builder recvWorld) throws IOException, ClassNotFoundException, SQLException{
        // UPSCommands.Builder recvUps = UPSCommands.newBuilder();
        // recvMesgFrom(recvUps, toups.getInputStream());
        ackToWorld(recvWorld);
        for(APurchaseMore x : recvWorld.getArrivedList()){
            //handle purchased item from world to warehouse
            worldPurchased(x);
        }
        for(APacked x : recvWorld.getReadyList()){
            //handle the packed package: packed->load
            worldPacked(x);
        }
        for(ALoaded x : recvWorld.getLoadedList()){
            //handle loaded truck from world to amazon: loaded->delivering
            worldLoaded(x);
        }
        for(AErr x : recvWorld.getErrorList()){
            //print the error message
            System.err.println("error msg:" + x.getErr());
        }
        for(APackage x : recvWorld.getPackagestatusList()){
            //get the status of package from world, change the status in Package
            package_list.get(x.getPackageid()).setStatus(x.getStatus());
        }
        //handle acks mechanism, for re-send
        for(long x : recvWorld.getAcksList()){
            if(rqst_list.containsKey(x)){
                rqst_list.get(x).cancel();
                rqst_list.remove(x);
            }
        }
        //handle disconnect
        if(recvWorld.hasFinished()){
            //connection disconnected, need to close the connection
            System.out.println("disconnect to world");
        }
    }

    void ackToWorld(AResponses.Builder recvWorld) throws IOException{
        List<Long> seqnum_list = new ArrayList<>();
        for(APurchaseMore x : recvWorld.getArrivedList()){
            seqnum_list.add(x.getSeqnum());
        }
        for(APacked x : recvWorld.getReadyList()){
            seqnum_list.add(x.getSeqnum());
        }
        for(ALoaded x : recvWorld.getLoadedList()){
            seqnum_list.add(x.getSeqnum());
        }
        for(AErr x : recvWorld.getErrorList()){
            seqnum_list.add(x.getSeqnum());
        }
        for(APackage x : recvWorld.getPackagestatusList()){
            seqnum_list.add(x.getSeqnum());
        }
        System.out.println("sending acks back to world");
        ACommands.Builder acommands = ACommands.newBuilder();
        if(seqnum_list.size() > 0){
            for(long x : seqnum_list){
                acommands.addAcks(x);
            }
            synchronized(toWorld.getOutputStream()){
                sendMesgTo(acommands.build(), toWorld.getOutputStream());
            }
        }
        //sendMesgTo(recvWorld.build(), toWorld.getOutputStream());
    }

    /*=======the world purchase products for warehouse====== */
    void worldPurchased(APurchaseMore x) throws IOException, ClassNotFoundException, SQLException{
        //需要synchronized吗？？？？？
        //find the package
        for(Package pkg : package_list.values()){
            if(pkg.getWarehouseid() != x.getWhnum()){
                continue;
            }
            if(!pkg.getAmazonPack().getThingsList().equals(x.getThingsList())){
                continue;
            }
            System.out.println("world purchased this item");
            //request trucks from UPS
            rqstTrucks(pkg);
            //request pack from world
            rqstTopack(pkg);
            break;
        }
    }

    //send request to UPS to send us trucks
    void rqstTrucks(Package pkg) throws IOException{
        long package_id = pkg.getPackageId();
        if(package_list.containsKey(package_id)){
            System.out.println("asking trucks");
            // Package new_pkg = package_list.get(package_id);
            //没有用thread处理？？？
            //get the sequence number
            long seqnum = getSeqNum();
            //create the A2UAskTruck rqst
            A2UAskTruck.Builder asktruck = A2UAskTruck.newBuilder();
            asktruck.setSeqnum(seqnum);
            asktruck.setWarehouse(AInintToWarehouse(warehouses.get(pkg.getWarehouseid())));
            //类型转换: Apack to Packageinfo
            //还没有对user name赋值????
            //destination
            int x = pkg.getDest().getX();
            int y = pkg.getDest().getY();
            asktruck.addPackage(APackToPackageinfo(pkg, x, y));

            //send A2UAskTruck rqst to ups
            AmazonCommands.Builder amazoncommand = AmazonCommands.newBuilder();
            amazoncommand.addGetTruck(asktruck);

            //send to UPS 不知道需不需要多个socket去处理，需要处理ack吗？？？？目前没有处理
            sendMesgTo(amazoncommand.build(), toups.getOutputStream());
            //sendAmazonCommands(amazoncommand.build());
        }else{
            //can not find the package according to id
            System.out.println("package for asking trucks does not exists!");
        }
    }

    //set the sequence number to each rqst
    synchronized long getSeqNum() {
        long cur = seqnum;
        seqnum++;
        return cur;
    }

    //send request to pack the package
    void rqstTopack(Package pkg) throws IOException, ClassNotFoundException, SQLException{
        long package_id = pkg.getPackageId();
        if(package_list.containsKey(package_id)){
            pkg.setStatus("packing");
            //没有写线程池取处理
            //threadPool.execute(() -> {
            //construct the Acommand
            ACommands.Builder acommand = ACommands.newBuilder();
            long seqnum = getSeqNum();
            APack apack = package_list.get(package_id).getAmazonPack();
            acommand.addTopack(apack.toBuilder().setSeqnum(seqnum));

            //send request Acommand to world 
            sendACommand(acommand.build(), seqnum);
            //});
        }
        else{
            //can not find the package according to id
            System.out.println("package for asking topack does not exists!");
        }
    }


    //+++++++++++++for type convert or assign some object+++++++++++++//
    //类型转换: Anitwarehouse to Warehouse
    Warehouse.Builder AInintToWarehouse(AInitWarehouse ainitwarehouse){
        Warehouse.Builder warehouse = Warehouse.newBuilder();
        warehouse.setWarehouseid(ainitwarehouse.getId());
        warehouse.setX(ainitwarehouse.getX());
        warehouse.setY(ainitwarehouse.getY());
        return warehouse;
    }

    //assign PackageInfo: Apack to Packageinfo
    PackageInfo.Builder APackToPackageinfo(Package p, int x, int y){
        PackageInfo.Builder packageinfo = PackageInfo.newBuilder();
        packageinfo.setShipid(p.getPackageId());
        packageinfo.setX(x);
        packageinfo.setY(y);
        if(p.getAcccount() != null){
            packageinfo.setUserName(p.getAcccount());
        }
        return packageinfo;
    }


    //send AmazonCommands to UPS
    // void sendAmazonCommands(AmazonCommands amazoncommands) throws IOException{
    //     //create a new socket to send AmazonCommands to ups
    //     //Socket toups_amazoncommds = new Socket(UPS_HOST, UPS_PORT);
    //     sendMesgTo(amazoncommands, toups.getOutputStream());
    //     //
    // }

    //send Acommand to world
    void sendACommand(ACommands accomands, long seqnum){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //为什么需要单独定义toWorld.getOutputStream()为out
                try {
                    synchronized (toWorld.getOutputStream()){
                        try {
                            sendMesgTo(accomands, toWorld.getOutputStream());
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }, 0, MAXTIME);
        //update the reqeust hash map
        rqst_list.put(seqnum, timer);
    }


    /*===========the world pack package for Amazon================*/
    void worldPacked(APacked x) throws ClassNotFoundException, SQLException{
        long package_id = x.getShipid();
        if(package_list.containsKey(package_id)){
            Package pkg = package_list.get(package_id);
            pkg.setStatus("packed");
            //if the truck is arrived, we can load the packages
            if(pkg.getTruckid() != -1 ){
                //start load the package
                worldPutOnTruck(pkg);
            }
        }
        else{
            //can not find the package according to id
            System.out.println("package for asking packing does not exists!");
        }
    }

    void worldPutOnTruck(Package pkg) throws ClassNotFoundException, SQLException{
        long package_id = pkg.getPackageId();
        if(package_list.containsKey(package_id)){
            pkg.setStatus("loading");
            //没有用线程池处理
            long seqnum = getSeqNum();
            APutOnTruck.Builder aputontruck = APutOnTruck.newBuilder();
            aputontruck.setWhnum(pkg.getWarehouseid());
            aputontruck.setTruckid(pkg.getTruckid());
            aputontruck.setShipid(package_id);
            aputontruck.setSeqnum(seqnum);

            ACommands.Builder acommand = ACommands.newBuilder();
            acommand.addLoad(aputontruck);

            //send Acommand to the world
            sendACommand(acommand.build(), seqnum);
        }
        else{
            //can not find the package according to id
            System.out.println("package for asking loading does not exists!");
        }
    }

    //loaded->delivering
    void worldLoaded(ALoaded x) throws IOException, ClassNotFoundException, SQLException{
        long package_id = x.getShipid();
        if(package_list.containsKey(package_id)){
            Package pkg = package_list.get(package_id);
            pkg.setStatus("loaded");
            //tell ups we loaded, send A2ULoaded and start deliver
            A2ULoaded.Builder loaded = A2ULoaded.newBuilder();
            long seqnum = getSeqNum();
            loaded.setSeqnum(seqnum);
            loaded.setWarehouse(AInintToWarehouse(warehouses.get(pkg.getWarehouseid())));
            loaded.setTruckid(pkg.getTruckid());
            AmazonCommands.Builder amazoncommand = AmazonCommands.newBuilder();
            amazoncommand.addLoaded(loaded);

            //send AmazonCommands to ups
            sendMesgTo(amazoncommand.build(), toups.getOutputStream());

            //set the status to "delivering"
            pkg.setStatus("delivering");
        }
        else{
            //can not find the package according to id
            System.out.println("package for loaded does not exists!");
        }
    }

    //for query from world
    // void queryToworld(){

    // }

    //for disconnect from world
    void disconnectFromworld(){
        ACommands.Builder acommand = ACommands.newBuilder();
        acommand.setDisconnect(true);
        sendACommand(acommand.build(), 0);
    }

    /*=========== interact with front-end ==============*/
    //thread for comm with front-end
    public void init_frontEndthread() throws IOException, ClassNotFoundException{
        while(!Thread.currentThread().isInterrupted()) {
            ServerSocket frontend_socket_for_listen = new ServerSocket(FRONT_PORT);
            //frontend_socket_for_listen.setSoTimeout(20000);
            System.out.println("start listening the connection request from front-end");
            while(!Thread.currentThread().isInterrupted()) {
                Socket frontend_socket_for_connect = frontend_socket_for_listen.accept();
                if(frontend_socket_for_connect != null){
                    //handle the requests from front-end 没写完
                    handle_frontend(frontend_socket_for_connect);
                }
            }
        }
    }

    //handle requests from front-end
    void handle_frontend(Socket frontend_socket) throws IOException, ClassNotFoundException{
        InputStreamReader input_reader = new InputStreamReader(frontend_socket.getInputStream());
        BufferedReader reader = new BufferedReader(input_reader);
        PrintWriter writer = new PrintWriter(frontend_socket.getOutputStream());
        String front_rqst = reader.readLine();
        System.out.println("the received front-end request is: " + front_rqst);
        //parse the package id
        long package_id = Long.parseLong(front_rqst);
        writer.write(String.format("received the package id: %d", package_id));
        writer.flush();

        frontend_socket.close();
        //handle the buy request, request the world to buy something for specific warehouse 没写完 
        try {
            worldBuy(package_id);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //world buy for warehouse，构造Apurchasemore
    void worldBuy(long id) throws SQLException, ClassNotFoundException{
        //没有用线程池处理 没写完
        dbProcess DB = new dbProcess();
        APurchaseMore.Builder apurchasemore = APurchaseMore.newBuilder();

        long seq_number = getSeqNum();
        apurchasemore.setSeqnum(seq_number);
        try {
            DB.construcBuyrqst(id, apurchasemore);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // long seq_number = getSeqNum();
        // apurchasemore.setSeqnum(seq_number);

        ACommands.Builder acommands = ACommands.newBuilder();
        acommands.addBuy(apurchasemore);
        //send the ACommand to world
        sendACommand(acommands.build(), seq_number);

        //update some info of the package
        APack.Builder apack = APack.newBuilder();
        int whnum = apurchasemore.getWhnum();
        apack.setWhnum(whnum);
        apack.addAllThings(apurchasemore.getThingsList());
        apack.setShipid(id);
        apack.setSeqnum(-1);

        //initialize the package list, since when backend receives the front-end's buy rqst
        //we rqst to world to buy for us, meanwhile we create a package
        Package pkg = new Package(whnum, id, apack.build());
        package_list.put(id, pkg);
    }
    


    /*=========== start all threads ===========*/
    void startAllthreads() throws IOException, ClassNotFoundException{
        Thread upsthread = new Thread(() -> {
            
            try {
                init_upsthread();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        upsthread.start();
        Thread worldthread = new Thread(() -> {
            try {
                init_worldthread();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        worldthread.start();
        // Thread worldthread = new Thread(() -> {
        //start the thread for comm with ups
        //init_upsthread();

        //start a thread for comm with world
        //init_worldthread();
        init_frontEndthread();
    }


    /*========== main function ==========*/
    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException{
        backfuncs backend = new backfuncs();
        //connect with ups
        backend.connect_ups();
        //run all threads
        backend.startAllthreads();
        //backend.init_frontEndthread();
    }


    
}

