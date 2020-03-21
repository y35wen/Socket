import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.*;
import java.util.regex.Pattern;

// Client program:
//  it will take four command line inputs:
//  <server_address>, <n_Port>, <req_code> and <msg> in the given order.

public class client {

// helper function to check if a string can convert to an integer.
    static boolean isInt(String s){
        try{
            int int_s = Integer.parseInt(s);
            return true;
        }catch (NumberFormatException ex){
            return false;
        }
    }

// helper function to check if 1st argument is a valid IP address
    static boolean isValidIP (String ip) {
        try{
            String [] parts = ip.split(Pattern.quote("."));
            String part1 = parts[0];
            String part2 = parts[1];
            String part3 = parts[2];
            String part4 = parts[3];
            int p1 = Integer.parseInt(part1);
            int p2 = Integer.parseInt(part2);
            int p3 = Integer.parseInt(part3);
            int p4 = Integer.parseInt(part4);
            if(p1>=0 && p1<= 255 & p2>=0 && p2<= 255 & p3>=0 && p3<= 255 & p4>=0 && p4<= 255){
                return true;
            } else{
                return false;
            }

        }catch (Exception e) {
            return false;
        }
    }

// main function
    public static void main(String[] args) throws Exception {

        // check arguments
        if(args.length!=4){
            System.err.println("Must have 4 arguments");
            System.exit(1);
        }
        if(isValidIP(args[0])==false){
            System.err.println("1st argument is not a valid IP address");
            System.exit(1);
        }
        if(isInt(args[1]) == false){
            System.err.println("2nd argument should be an integer");
            System.exit(1);
        }
        if(isInt(args[2]) == false){
            System.err.println("3rd argument should be an integer");
            System.exit(1);
        }
        if(args[3].isEmpty()==true){
            System.err.println("Invalid Input for the 4th Argument ");
            System.exit(1);
        }

        //fields
        DatagramSocket clientUDPSocket; //stage 1 socket
        Socket clientTCPSocket; // stage 2 socket
        String server_address = args[0]; // 1st command line parameter: <server_address>
        String nport = args[1];  // 2nd command line parameter: <n_port>, UDP socket port number
        String reqCode = args[2];  // 3rd command line parameter: <req_code>
        String msg = args[3];  // 4th command line parameter: <msg>
        int r_port; // TCP socket port number

//////////        Stage 1: Negotiation using UDP sockets
        // 1. Client creates a UDP socket with the server using <server_address> as the server address and
        //      <n_port> as the negotiation port on the server
        clientUDPSocket = new DatagramSocket();
        int negotiation_port = Integer.parseInt(nport);
        InetAddress server_addr = InetAddress.getByName(server_address);

        // 2. The client sends a request code <req_code>, an integer, through the UDP socket.
        sendReq_code(reqCode,clientUDPSocket,server_addr,negotiation_port);

        // 3. if the client fails to send the intended <req_code>, the server does nothing
        //  if success, receive the <r_port>
        byte[] buffer = new byte[1024];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        clientUDPSocket.receive(response);
        r_port = Integer.parseInt(new String(buffer,0,response.getLength()));

        // 4. send another UDP message to confirm <r_port>
        confirmR_Port(clientUDPSocket,r_port,server_addr,negotiation_port);

        // 5. get acknowledges that the received r_port is
        Acknowledges(clientUDPSocket);

        /////////////      Stage 2: Transaction using TCP sockets
        //  client creates a TCP connection to the server and start the transaction
        clientTCPSocket = new Socket(server_address, r_port);
        Transaction(clientTCPSocket,msg);
    }


    static void sendReq_code(String reqCode, DatagramSocket clientUDPSocket, InetAddress server_addr, int negotiation_port){
        try {
            byte[] Buffer = reqCode.getBytes();
            DatagramPacket request = new DatagramPacket(Buffer, Buffer.length, server_addr, negotiation_port);
            clientUDPSocket.send(request);
        }catch (Exception e){
            System.err.println("client can NOT send <req_code>");
            System.exit(1);
        }
    }

    static  void confirmR_Port(DatagramSocket clientUDPSocket,int r_port,InetAddress server_addr, int negotiation_port){
        try{
            byte[] Buffer = String.valueOf(r_port).getBytes();
            DatagramPacket request = new DatagramPacket(Buffer, Buffer.length,server_addr,negotiation_port);
            clientUDPSocket.send(request);
        }catch (Exception e){
            System.err.println("client can NOT send msg to confirm about the <r_port>");
            System.exit(1);
        }
    }

    static void Acknowledges(DatagramSocket clientUDPSocket){
        try{
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            clientUDPSocket.receive(response);
            String confirm = new String(buffer,0,response.getLength());
            if(confirm.equals("ok")) {
                clientUDPSocket.close();
            }
            else{
                System.err.println("r_port incorrect");
                clientUDPSocket.close();
                System.exit(1);
            }
        }catch (Exception e){
            System.err.println("client can NOT get acknowledge from the server about <r_port>");
            System.exit(1);
        }
    }

    static void Transaction(Socket clientTCPSocket,String msg) {
        // 1. The client sends the <msg> containing a string
        try {
            PrintWriter out = new PrintWriter(clientTCPSocket.getOutputStream(), true);
            out.println(msg);
            out.flush();
        } catch (Exception e){
            System.err.println("can not send <msg> to server ");
            System.exit(1);

        }
        // 2. Once received, the client prints out the reversed string and exits
        try {
            String receivedMsg;
            BufferedReader in = new BufferedReader(new InputStreamReader(clientTCPSocket.getInputStream()));
            receivedMsg = in.readLine();
            System.out.println("CLIENT_RCV_MSG='" + receivedMsg + "'");
            clientTCPSocket.close();
        }catch (Exception e){
            System.err.println("client can NOT receive the reversed string ");
            System.exit(1);
        }
    }

}