/*~*~*~*~*~*~*~*~*~*~*~*~*~*/
/*                         */
/*201605791 David Fernandes*/
/*201606816 João Silva     */
/*                         */
/*~*~*~*~*~*~*~*~*~*~*~*~*~*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    private Scanner stdin;
    private PrintStream p;
    private Socket connection;
    private String message = "";


    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }


    // Construtor
    public ChatClient() throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                }catch(IOException ex) {
                }finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocus();
            }

            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
              if(JOptionPane.showConfirmDialog(frame, "Tem a certeza que quer sair?", "Sair do Chat?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
                p.print("/bye\n");
                System.exit(0);
              }
            }

        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
          p.print(message + '\n');
    }


    // Método principal do objecto
    public void run(String server, int port) throws IOException {
        try {
          connectToServer(server, port);
          setupStreams();
          whileChatting();
        }catch(EOFException eofException) {
          printMessage("\nClient ended connection\n");
        }catch(IOException ex) { }
    }

    // Método para conectar ao servidor
    public void connectToServer(String server, int port) throws IOException {
      printMessage("\nAttempting connection...\n");
      connection = new Socket(server, port);
      printMessage("\nConnected to: " + connection.getInetAddress().getHostName() + "\n");
    }

    // Método para inicializar streams para enviar e receber mensagens
    public void setupStreams() throws IOException {
      p = new PrintStream(connection.getOutputStream());
      stdin = new Scanner(connection.getInputStream());
    }

    //Enquanto conversa com o servidor
    public void whileChatting() throws IOException {
      while(true) {
        try {
          message = stdin.nextLine();
          if(message.equals("BYE\n")) {
            System.out.println("lew");
            return;
          }
          else if(message.startsWith("MESSAGE")) {
            String[] newMess = message.split("\\s");
            message = message.replace("MESSAGE " + newMess[1], "");
            printMessage(newMess[1] + ": " + message + "\n");
          }
          else if(message.startsWith("PRIVATE")) {
            String[] newMess = message.split("\\s");
            message = message.replace("PRIVATE " + newMess[1], "");
            printMessage("(privada) " + newMess[1] + ": " + message + "\n");
          }
          else if(message.startsWith("NEWNICK")) {
            String[] newMess = message.split("\\s");
            printMessage(newMess[1] + " mudou de nome para " + newMess[2] + "\n");
          }
          else if(message.startsWith("JOINED")) {
            String[] newMess = message.split("\\s");
            printMessage(newMess[1] + " entrou no chat" + "\n");
          }
          else if(message.startsWith("LEFT")) {
            String[] newMess = message.split("\\s");
            printMessage(newMess[1] + " abandonou o chat" + "\n");
          }
          else if(message.startsWith("ERROR")) {
            printMessage("Comando incorreto!" + "\n");
          }
          else printMessage(message + "\n");
        }catch(NoSuchElementException nsex) {

          try{
            Thread.sleep(1000);
          }catch(InterruptedException ex){
            Thread.currentThread().interrupt();
          }
          System.exit(0);
        }
      }
    }


    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient();
        client.run(args[0], Integer.parseInt(args[1]));
    }

}
