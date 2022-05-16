import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;

public class server {
    public static void main(String arg[]) throws Exception {
        
        int total=Runtime.getRuntime().availableProcessors();
        if(total<=0){
            total=1;
        }
        tmanager[] tman = new tmanager[total];
        for(int i=0;i<total;i++){
            tman[i]=new tmanager();
            tman[i].start();
        }

        int counts=0;
        ServerSocket ss=new ServerSocket(0);
        System.out.println("Server is listening on port "+ss.getLocalPort());
        while(true){
            for(int i=0;i<total;i++){
                try {
                    Socket s = ss.accept();

                    
                        tman[i].addnewScok(s);
                        
                    
                }catch (Exception e){}
            }
        }

    }
    private static class ProxyDetails{
        private Socket clientsock,destinationsock;
        private InputStream is,destinationInput;
        private OutputStream os,destinationOutput;
        private String destinationip;
        private int port=0;
        private byte version;
        private byte command;
        private String reconnectip;
        private int reconnectport;
        private boolean isUdp=false;
        private boolean isDataUpdated=false;
        public ProxyDetails(Socket clientsock) throws Exception {
            this.clientsock = clientsock;
            
            this.os=clientsock.getOutputStream();
            this.is=clientsock.getInputStream();

        }
        public void setBind(String reconnectip,int reconnectport)
        {
            this.reconnectip=reconnectip;
            this.reconnectport=reconnectport;
        }

 

        public void updateDestination(Socket destinationsock) throws Exception {
            this.destinationsock=destinationsock;
            this.destinationInput=this.destinationsock.getInputStream();
            this.destinationOutput=this.destinationsock.getOutputStream();
            this.isDataUpdated=true;
            if(this.command==0x01) {
                if(this.version==0x04) {
                    byte[] REPLY = new byte[8];
                    int port = destinationsock.getPort();

                    byte[] ip = destinationsock.getInetAddress().getAddress();
                    REPLY[0] = 0;
                    REPLY[1] = 90;
                    //int port to byte convert
                    REPLY[2] = (byte) ((port & 0xFF00) >> 8);
                    REPLY[3] = (byte) (port & 0x00FF);
                    //ip in byte
                    REPLY[4] = ip[0];
                    REPLY[5] = ip[1];
                    REPLY[6] = ip[2];
                    REPLY[7] = ip[3];

                    getClientOutput().write(REPLY);
                    getClientOutput().flush();

                }else{
                    int port = destinationsock.getLocalPort();
                    byte[] ip = destinationsock.getInetAddress().getAddress();
                    byte REPLY[]=new byte[10];
                    REPLY[0] = 0x05;
                    REPLY[1] = 00;
                    REPLY[2] = 0x00;        // Reserved	'00'
                    REPLY[3] = 0x01;        // DOMAIN NAME Address Type IP v4
                    REPLY[4] = ip[0];
                    REPLY[5] = ip[1];
                    REPLY[6] = ip[2];
                    REPLY[7] = ip[3];
                    REPLY[8] = (byte) ((port & 0xFF00) >> 8);// Port High
                    REPLY[9] = (byte) (port & 0x00FF);

                    getClientOutput().write(REPLY);
                    getClientOutput().flush();

                }
            }
        }

        public void updateData(String destinationip,int port,byte command,byte version) throws Exception {
            this.destinationip=destinationip;
            this.version=version;
            this.port=port;
            this.command=command;



        }


        public void setUdp(boolean udp) {
            isUdp = udp;
        }

        public boolean isUdp() {
            return isUdp;
        }

        public InputStream getDestinationInput() {
            return destinationInput;
        }

        public OutputStream getDestinationOutput() {
            return destinationOutput;
        }

        public Socket getClientsock() {
            return clientsock;
        }

        public void setClientsock(Socket clientsock) {
            this.clientsock = clientsock;
        }

        public Socket getDestinationsock() {
            return destinationsock;
        }

        public void setDestinationsock(Socket destinationsock) {
            this.destinationsock = destinationsock;
        }

        public InputStream getClientInput() {
            return is;
        }

        public OutputStream getClientOutput() {
            return os;
        }

        public String getDestinationip() {
            return destinationip;
        }

        public int getDestinationPort() {
            return port;
        }

        public byte getCommand() {
            return command;
        }

        public boolean isDataUpdated() {
            return isDataUpdated;
        }
    }
    private static class tmanager extends Thread{
        private ArrayList<ProxyDetails>socks;

        public tmanager(){
            socks=new ArrayList<>();
        }
        @Override
        public void run(){
            byte chunk[]=new byte[1024];
            int size;
            while(true){
                try {
                    if (socks.size() <= 0) {
                        Thread.sleep(10);
                        continue;
                    }
                    for(int i=0;i<socks.size();i++){
                        ProxyDetails pd=socks.get(i);
                        if(pd.isDataUpdated){
                            if(pd.isUdp()){
//                                TODO need to add udp
                            }else {
                                try {
                                    if(pd.getClientInput().available()!=0) {
                                        size = pd.getClientInput().read(chunk, 0, 1024);
                                        if (size > -1) {
                                            if (size != 0) {
                                                pd.getDestinationOutput().write(chunk, 0, size);
                                                pd.getDestinationOutput().flush();
                                            }
                                        } else {
                                            close(i);
                                            i--;
                                            continue;
                                        }
                                    }

                                }catch (Exception e){
                                    
                                    close(i);
                                    i--;
                                    continue;
                                }

                                try {
                                    if(pd.getDestinationInput().available()!=0) {
                                        size = pd.getDestinationInput().read(chunk, 0, 1024);
                                        if (size > -1) {
                                            if (size != 0) {
                                                pd.getClientOutput().write(chunk, 0, size);
                                                pd.getClientOutput().flush();
                                            }
                                        } else {
                                            close(i);
                                            i--;
                                            continue;
                                        }
                                    }
                                }catch (Exception e ){
                                    close(i);
                                    i--;
                                    continue;
                                }

                            }
                        }else{
                            try {
                                addOtherdetails(i);
                            }catch (Exception e){
                                pd.getClientOutput().write((byte) 92);
                                pd.getClientOutput().flush();
                            }
                        }
                    }
                }catch (Exception ex){

                }
            }
        }
        private void close(int i){
            ProxyDetails pd=socks.get(i);
            try {
                pd.getClientInput().close();
            }catch (Exception e){}
            try {
                pd.getClientOutput().close();
            }catch (Exception e){}
            try {
                pd.getDestinationOutput().close();
            }catch (Exception e){}
            try {
                pd.getDestinationInput().close();
            }catch (Exception e){}
            try {
                pd.getClientsock().close();
            }catch (Exception e){}
            try {
                pd.getDestinationsock().close();
            }catch (Exception e){}
            socks.remove(i);
        }
        private byte readByte(InputStream is) throws IOException {
            return (byte)is.read();
        }
        private int byte2int(byte b) {
            return (int) b < 0 ? 0x100 + (int) b : b;
        }

        private int calcPort(byte Hi, byte Lo) {
            return ((byte2int(Hi) << 8) | byte2int(Lo));
        }
        private void addOtherdetails(int i) throws Exception {
            ProxyDetails pd=socks.get(i);
            InputStream is=pd.getClientInput();
            byte data=readByte(is);
            int port=0;
            byte command;
            String host=null;
            if(data==0x05){
                //version of socket
                
                authenticate(pd);
                readByte(is);

                command = readByte(is);
                readByte(is); // Reserved. Must be'00'
                byte adtype = readByte(is);

                if(adtype==0x03){
                    //domain name
                    int len=readByte(is);
                    String domain="";
                    for(int ij=0;ij<len;ij++){
                        domain+=((char)readByte(is));
                    }
                    port =calcPort(readByte(is),readByte(is));
                    host=domain;
                }else if(adtype==0x01){
                    //ipv4
                    int len=4;
                    String domain=""+byte2int(readByte(is));
                    for(int ij=1;ij<len;ij++){
                        domain+="."+byte2int(readByte(is));
                    }
                    port =calcPort(readByte(is),readByte(is));
                    host=domain;
                }else{
                    throw new Exception("Invalid address");
                }
                pd.updateData(host,port,command,data);
                
            }else if(data ==0x04){
                
                command = readByte(is);
                port =calcPort(readByte(is),readByte(is));
                host=""+byte2int(readByte(is));
                for (int ij = 1; ij < 4; ij++) {
                    host+= "."+byte2int(readByte(is));
                }
                boolean flag=true;
                for(int ijk=0;ijk<10000;ijk++) {
                    if (readByte(is) == 0x00) {
                        flag=false;
                        break;
                    }
                }
                if(flag){
                    close(i);
                    throw new Exception("Invalid version");
                }
                socks.get(i).updateData(host,port,command,data);
                

            }else{
                throw new Exception("Invalid version");
            }
//            TODO need to update destination sock
            if(pd.getCommand()==0x01){
                //connect and transfer data
                    
                    InetAddress ia = InetAddress.getByName(pd.getDestinationip());
                    Socket dessock=new Socket();
                    
                    dessock.connect(new InetSocketAddress(ia,pd.getDestinationPort()));

                    pd.updateDestination(dessock);

            }else if(pd.getCommand()==0x02){
                //bind with new port
                
                throw new Exception("We don't want to program that for now.");
            }else if(pd.getCommand()==0x03){
                //initialize udp
                
                pd.setUdp(true);
            }else{
                throw new Exception("Unknown command.");
            }

        }
        public void authenticate(ProxyDetails pd) throws Exception {
//            need to update for socks5
            InputStream is=pd.getClientInput();
            byte len=readByte(is);
            String data="";
            for(int i=0;i<len;i++){
                data+=",-"+readByte(is)+"-";
            }
            pd.getClientOutput().write(new byte[]{(byte) 0x05, (byte) 0x00});
            pd.getClientOutput().flush();

        }
        public void addnewScok(Socket sock) throws Exception {
            socks.add(new ProxyDetails(sock));
        }
    }

}
