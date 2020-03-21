import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.*;


// Server program:
//  The server will take <req_code> as a command line parameter.
//  The server must print out the <n_Port> value


public class server {


// helper function to check if a string can convert to an integer.
    static boolean isInt(String s){
        try{
            int int_s = Integer.parseInt(s);
            return true;
        }catch (NumberFormatException ex){
            return false;
        }
    }

// main function
    public static void main(String[] args) throws Exception {

        //check arguments
        if(args.length!=1){
            System.err.println("Only one common line argument for server");
            System.exit(1);
        }
        if(false == isInt(args[0])){
            System.err.println("Not a number");
            System.exit(1);
        }

        //fields
        DatagramSocket serverUDPSocket; //stage 1 socket
        Socket serverTCPSocket; // stage 2 socket
        String reqCode = args[0];  // common line parameter
        int req_code = Integer.parseInt(reqCode);  // req_code in integer form
        int n_port = 44862;  // UDP socket port number
        int r_port;          // TCP socket port number

/////////         Stage1: Negotiation using UDP sockets
        // 1. The server creates a UDP socket with <n_port> and start listening on this port
        n_port = new ServerSocket(0).getLocalPort(); // find a free port number
        serverUDPSocket = new DatagramSocket(n_port);
        System.out.println("SERVER_PORT=" + n_port );
        while(1==1) {

            // 2. The server receives and verifies the <req_code>
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            serverUDPSocket.receive(response);
            String receivedReq_code = new String(buffer,0,response.getLength());
            int received_req_code = Integer.parseInt(receivedReq_code);    // receive the <req_code>
            if (req_code != received_req_code) { // if the client fails to send intended <req_code>,
                System.err.println("req_code are not same");
                break;                         //    the server does nothing.
            }

            // the <req_code> is verified, and the server creates a TCP sockets with a random port number <r_port>
            ServerSocket TCPSocket = new ServerSocket(0);  // Use ServerSocket to allocate a free port
            r_port = TCPSocket.getLocalPort();
            System.out.println("SERVER_TCP_PORT=" + r_port);

            // 3. It then uses the UDP socket to reply back with <r_port>
            byte[] Buffer = Integer.toString(r_port).getBytes();
            InetAddress client_address = response.getAddress(); // the server address received from Client
            int client_port = response.getPort(); //the port that received from Client
            DatagramPacket request = new DatagramPacket(Buffer, Buffer.length, client_address, client_port);
            serverUDPSocket.send(request);

            // 4. The server then acknowledges that the received <r_port> at the client is correct using UDP message.
            buffer = new byte[1024];
            response = new DatagramPacket(buffer, buffer.length);
            serverUDPSocket.receive(response);
            int received_rport = Integer.parseInt(new String(buffer,0,response.getLength()));
            if (received_rport == r_port) {
                Buffer = "ok".getBytes();
                request = new DatagramPacket(Buffer, Buffer.length, client_address, client_port);
                serverUDPSocket.send(request);
                serverTCPSocket = TCPSocket.accept();
            } else {
                Buffer = "no".getBytes();
                request = new DatagramPacket(Buffer, Buffer.length, client_address, client_port);
                serverUDPSocket.send(request);
                serverUDPSocket.close();
                System.err.println("r_port not right");
                System.exit(1);
                break;
            }

            //////////////        Stage 2: Transaction using TCP sockets
            transaction(serverTCPSocket);
        }
    }


    static void transaction(Socket serverTCPSocket){
        try{
            // 1. the server receives the string
            String receivedMsg;
            BufferedReader in = new BufferedReader(new InputStreamReader(serverTCPSocket.getInputStream()));
            receivedMsg = in.readLine();
            System.out.println("SERVER_RCV_MSG='" + receivedMsg+"'");

            // 2. and sends the reversed string back to the client
            StringBuilder sb = new StringBuilder(receivedMsg);
            String reversedMsg = sb.reverse().toString();
            PrintWriter out = new PrintWriter(serverTCPSocket.getOutputStream(), true);
            out.println(reversedMsg);
            out.flush();
        } catch (Exception e) {
            System.err.println("the server can not do the transaction stage ");
            System.exit(1);
        }
    }

}