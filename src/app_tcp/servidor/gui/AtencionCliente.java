package app_tcp.servidor.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;

/**
 *
 * @author stivenrodriguez
 */
public class AtencionCliente extends Thread {

    private final Socket clientSocket;
    private final JTextArea mensajesTxt;
    private final List<AtencionCliente> listaClientes;
    private PrintWriter out;

    public AtencionCliente(Socket conexion, JTextArea mensajesTxt, List<AtencionCliente> listaClientes) {
        this.clientSocket = conexion;
        this.mensajesTxt = mensajesTxt;
        this.listaClientes = listaClientes;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            String linea;
            while ((linea = in.readLine()) != null) {
                mensajesTxt.append("Cliente: " + linea + "\n");
                System.out.println("Mensaje del cliente: " + linea);

                // Reenviar el mensaje a todos los demás clientes
                enviarATodos(linea);
            }
        } catch (IOException ex) {
            mensajesTxt.append("Error al comunicar con el cliente: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                mensajesTxt.append("Error al cerrar el socket: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
            synchronized (listaClientes) {
                listaClientes.remove(this);
            }
        }
    }

    public void enviarMensaje(String mensaje) {
        out.println(mensaje);
    }

    private void enviarATodos(String mensaje) {
        for (AtencionCliente cliente : listaClientes) {
            if (cliente != this) { // No enviar el mensaje al cliente que lo envió
                cliente.enviarMensaje("Mensaje de otro cliente: " + mensaje);
            }
        }
    }
}
