/*~*~*~*~*~*~*~*~*~*~*~*~*~*/
/*                         */
/*201605791 David Fernandes*/
/*201606816 João Silva     */
/*                         */
/*~*~*~*~*~*~*~*~*~*~*~*~*~*/

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class User {
  private String nick;    // Nickname do utilizador
  private String room;    // Sala
  private String state;   // Estado
  private String command; // String auxiliar para quando se usa <CTRL-D> no ncat

  User(String nick) {
    this.nick = nick;
    this.state = "init";
  }

  void setNick(String nick) {
    this.nick = nick;
  }

  String getNick() {
    return this.nick;
  }

  void setRoom(String room) {
    this.room = room;
  }

  String getRoom() {
    return this.room;
  }

  void setState(String state) {
    this.state = state;
  }

  String getState() {
    return this.state;
  }

  void setCmd(String command) {
    this.command = command;
  }

  String getCmd() {
    return this.command;
  }
}

public class ChatServer {
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate(16384);
  static private String[] cmd_list;
  static private int offset = 0;

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();

  static private HashSet<String> users = new HashSet<>();
  static private HashSet<String> rooms = new HashSet<>();

  static private Selector selector;

  static public void main(String args[]) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt(args[0]);

    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking(false);

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress(port);
      ss.bind(isa);

      // Create a new Selector for selecting
      selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register(selector, SelectionKey.OP_ACCEPT);
      System.out.println("Listening on port " + port);

      while(true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if(num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while(it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if(key.isAcceptable()) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println("Got connection from " + s);

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking(false);

            // Register it with the selector, for reading
            sc.register(selector, SelectionKey.OP_READ);

          }else if(key.isReadable()) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              boolean ok = processInput(sc, key);

              // If the connection is dead, remove it from the selector
              // and close it
              if(!ok) {
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println("Closing connection to " + s);
                  s.close();
                }catch(IOException ie) {
                  System.err.println("Error closing socket " + s + ": " + ie);
                }
              }

            }catch(IOException ie) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              }catch(IOException ie2) { System.out.println(ie2); }

              System.out.println("Closed " + sc);
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    }catch(IOException ie) {
      System.err.println(ie);
    }
  }


  static private boolean processInput(SocketChannel sc, SelectionKey key) throws IOException {
    // Read the message to the buffer
    buffer.clear();
    sc.read(buffer);
    buffer.flip();

    // If no data, close the connection
    if (buffer.limit() == 0) {
      return false;
    }


    User user = (User) key.attachment();
    String message = decoder.decode(buffer).toString();


    // Para evitar que a mensagem seja enviada com o CTRL-D
    if(!message.endsWith("\n") && user == null) {
      User u = new User(null);
      u.setCmd(message);
      key.attach(u);
      return true;
    }
    else if(!message.endsWith("\n") && user != null && user.getCmd() == null) {
      user.setCmd(message);
      return true;
    }
    else if(!message.endsWith("\n") && user != null && user.getCmd() != null) {
      user.setCmd(user.getCmd() + message);
      return true;
    }
    else if(message.endsWith("\n") && user != null && user.getCmd() != null) {
      message = user.getCmd() + message;
      if(user.getNick() == null){
        user.setCmd(null);
        user = null;
        key.attach(user);
      }
      else {
        user.setCmd(null);
      }
    }

    //Para poder ler ficheiros tem um ciclo while
    while(true) {
      user = (User) key.attachment();
      if(cmd_list == null) {
        String aux[] = message.split("\n");
        if(aux.length > 1){
          cmd_list = Arrays.copyOfRange(aux, 1, aux.length);
          message = aux[0] + "\n";
        }
      }
      else if(cmd_list != null && cmd_list.length > 0) {
        message = cmd_list[0] + "\n";
        cmd_list = Arrays.copyOfRange(cmd_list, 1, cmd_list.length);
      }

      //login
      if(message.startsWith("/nick") && user == null) {
        String[] aux = message.split("\\s");
        if(aux.length < 2) replyERROR(sc);
        else {
          message = message.replaceAll("\\s", "");
          String nick = message.replace("/nick", "");
          if(!users.contains(nick)) {
            users.add(nick);
            User u = new User(nick);
            u.setState("outside");
            key.attach(u);
            replyOK(sc);
            System.out.println(nick + " logged in");
          }
          else {
            replyERROR(sc);
          }
        }
      }

      //mudanca de nick fora de sala
      else if(message.startsWith("/nick") && user.getState().equals("outside")) {
        String[] aux = message.split("\\s");
        if(aux.length < 2) replyERROR(sc);
        else {
          message = message.replaceAll("\\s", "");
          String nick = message.replace("/nick", "");
          String old_nick = user.getNick();
          if(!users.contains(nick)) {
            users.remove(old_nick);
            users.add(nick);
            user.setNick(nick);
            replyOK(sc);
            System.out.println(old_nick + " changed Nickname to " + nick);
          }
          else {
            replyERROR(sc);
          }
        }
      }

      //mudanca de nick dentro de sala
      else if(message.startsWith("/nick") && user.getState().equals("inside")) {
        String[] aux = message.split("\\s");
        if(aux.length < 2) replyERROR(sc);
        else {
          message = message.replaceAll("\\s", "");
          String nick = message.replace("/nick", "");
          String old_nick = user.getNick();
          String room = user.getRoom();
          if(!users.contains(nick)) {
            users.remove(old_nick);
            users.add(nick);
            user.setNick(nick);
            replyOK(sc);
            System.out.println(old_nick + " changed Nickname " + nick);
            user.setRoom(null);
            broadcast("NEWNICK " + old_nick + " " + nick + "\n", room);
            user.setRoom(room);
          }
          else {
            replyERROR(sc);
          }
        }
      }

      //ingresso numa sala
      else if(message.startsWith("/join") && user != null && user.getState().equals("outside")) {
        String[] aux = message.split("\\s");
        if(aux.length < 2) replyERROR(sc);
        else {
          message = message.replaceAll("\\s", "");
          String room = message.replace("/join", "");
          if(!rooms.contains(room)) {
            rooms.add(room);
          }
          user.setState("inside");
          replyOK(sc);
          broadcast("JOINED " + user.getNick() + "\n", room);
          user.setRoom(room);
          System.out.println(user.getNick() + " joined room " + room);
        }
      }

      //mudanca de sala
      else if(message.startsWith("/join") && user != null && user.getState().equals("inside")) {
        String[] aux = message.split("\\s");
        if(aux.length < 2) replyERROR(sc);
        else {
          message = message.replaceAll("\\s", "");
          String room = message.replace("/join", "");
          String old_room = user.getRoom();
          if (!rooms.contains(room)) {
            rooms.add(room);
          }
          replyOK(sc);
          user.setRoom(null);
          if(checkRoom(old_room)){
            broadcast("LEFT " + user.getNick() + "\n", old_room);
          }
          else {
            rooms.remove(room);
          }
          broadcast("JOINED " + user.getNick() + "\n", room);
          user.setRoom(room);
          System.out.println(user.getNick() + " changed from room " + old_room + " to " + room);
        }
      }

      //saida de uma sala
      else if(message.replaceAll("\\s", "").equals("/leave") && user != null && user.getState().equals("inside")) {
        replyOK(sc);
        users.remove(key.attachment());
        user.setState("outside");
        String room = user.getRoom();
        user.setRoom(null);
        broadcast("LEFT " + user.getNick() + "\n", room);
        System.out.println(user.getNick() + " left room " + room);
      }

      //logout
      else if(message.replaceAll("\\s", "").equals("/bye")) {
        String rply = "BYE\n";
        buffer.clear();
        buffer.put(rply.getBytes());
        buffer.flip();
        sc.write(buffer);
        String room = "";
        if(user != null && user.getState().equals("inside")){
          room = user.getRoom();
          user.setRoom(null);
          broadcast("LEFT " + user.getNick() + "\n", room);
          users.remove(user.getNick());
          user.setState("");
        }
        else if(user != null && user.getState().equals("outside")) {
          users.remove(user.getNick());
          user.setState("");
        }
        return false;
      }

      //comunicacao privada
      else if(message.startsWith("/priv") && user!= null && user.getState().equals("inside")) {
        String[] aux = message.split("\\s");
        if(aux.length < 3) replyERROR(sc);
        else {
          message = message.replace("/priv " + aux[1], "");
          String rply = "PRIVATE " + user.getNick() + " " + message;
          if(!privateMessage(rply, aux[1], user.getRoom())) {
            replyERROR(sc);
          }
          else {
            buffer.clear();
            buffer.put(rply.getBytes());
            buffer.flip();
            sc.write(buffer);
          }
        }
      }

      //escape de / adicionais
      else if(message.startsWith("//") && user!= null && user.getState().equals("inside")) {
        message = message.substring(1);
        String rply = "MESSAGE " + user.getNick() + " " + message;
        buffer.clear();
        buffer.put(rply.getBytes());
        buffer.flip();
        broadcast(rply, user.getRoom());
      }

      //comunicacao dentro de sala
      else if(user != null && user.getState().equals("inside")) {
        String rply = "MESSAGE " + user.getNick() + " " + message;
        buffer.clear();
        buffer.put(rply.getBytes());
        buffer.flip();
        broadcast(rply, user.getRoom());
      }


      else {
        replyERROR(sc);
      }

      if(cmd_list == null || cmd_list != null && cmd_list.length == 0) {
        if(!message.startsWith("/bye") && cmd_list != null){
          user.setRoom(null);
          users.remove(user.getNick());   //se o ficheiro nao acabar com bye
          user.setState("");
        }
        cmd_list = null;
        return true;
      }
    }
  }

  static private void replyOK(SocketChannel sc) throws IOException {
    String rply = "OK\n";
    buffer.clear();
    buffer.put(rply.getBytes());
    buffer.flip();
    sc.write(buffer);
  }

  static private void replyERROR(SocketChannel sc) throws IOException {
    String rply = "ERROR\n";
    buffer.clear();
    buffer.put(rply.getBytes());
    buffer.flip();
    sc.write(buffer);
  }

  //verifica se a sala tem utilizadores
  static private boolean checkRoom(String Room) throws IOException {
		for(SelectionKey key : selector.keys()) {
      User user = (User) key.attachment();
			if(key.isValid() && key.channel() instanceof SocketChannel && user != null && user.getRoom() != null && user.getRoom().equals(Room)) {
        return true;
			}
		}
    return false;
	}

  //enviar a mensagem aos restantes utilizadores da sala
  static private void broadcast(String msg, String Room) throws IOException {
		ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());
		for(SelectionKey key : selector.keys()) {
      User user = (User) key.attachment();
			if(key.isValid() && key.channel() instanceof SocketChannel && user != null && user.getRoom() != null && user.getRoom().equals(Room)) {
				SocketChannel sch = (SocketChannel) key.channel();
				sch.write(msgBuf);
				msgBuf.rewind();
			}
		}
	}

  static private boolean privateMessage(String msg, String dest, String Room) throws IOException {
		ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());
    boolean flag = false;
		for(SelectionKey key : selector.keys()) {
      User user = (User) key.attachment();
			if(key.isValid() && key.channel() instanceof SocketChannel && user != null && user.getNick().equals(dest) && user.getRoom() != null && user.getRoom().equals(Room)) {
				SocketChannel sch = (SocketChannel) key.channel();
				sch.write(msgBuf);
				msgBuf.rewind();
        flag = true;
			}
		}
    if(!flag) return false;
    return true;
	}
}
