# Redes-de-Comunicacao-2019-2020-Trabalho
Chat simples / Simple chat

Para executar / To execute:
1) $javac *.java
2) $java ChatServer <Escolher porto/Choose port>
3) Abra outro terminal / Open another terminal.
4) $java ChatClient localhost <porto escolhida em 2)/port chosen in 2)>
5) Para clientes adicionais repetir os passos a partir de 3) / For additional clients repeat all steps after 3.

Comandos / Commands

/nick <nome/name>
Usado para escolher um nome ou para mudar de nome. O nome escolhido não pode estar já a ser usado por outro utilizador.
Used to choose or change a name. The name must be unique.

/join <sala/chat room>
Usado para entrar numa sala de chat ou para mudar de sala. Se a sala ainda não existir, é criada.
Used to enter a chat room or to change to another. If the room does not exist, it is created.

/leave
Usado para o utilizador sair da sala de chat em que se encontra.
Leave the chat room.

/bye
Usado para sair do chat.
Close chat.

Nota / Note: De modo a enviar um comando para outro utilizador sem que esta seja reconhecida por o servidor é necessário introduzir duas barras antes do comando.

e.g: //leave

