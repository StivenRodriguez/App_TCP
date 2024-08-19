package app_tcp.servidor.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import javax.swing.JTextArea;

/**
 *
 * @author stivenrodriguez
 */
public class AtencionCliente extends Thread {

    private String nombreCliente;
    private String idClienteSocket;
    private final Socket clientSocket;
    private final JTextArea mensajesTxt;
    private final List<AtencionCliente> listaClientes;
    private PrintWriter out;

    public AtencionCliente(String nombreCliente, Socket clientSocket, JTextArea mensajesTxt, List<AtencionCliente> listaClientes) {
        this.nombreCliente = nombreCliente;
        this.clientSocket = clientSocket;
        this.mensajesTxt = mensajesTxt;
        this.listaClientes = listaClientes;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getIdClienteSocket() {
        return idClienteSocket;
    }

    public void setIdClienteSocket(String idClienteSocket) {
        this.idClienteSocket = idClienteSocket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            String linea;
            while ((linea = in.readLine()) != null) {
                if(linea.equals("enviar clientes activos")){
                    //Solcitar clientes activos
                    enviarListaClientesATodos();
                }
                
                mensajesTxt.append("Cliente: " + this.nombreCliente + "\n");
                System.out.println("Mensaje: " + linea + "\n");
                procesarMensaje(linea);
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
    
    private void enviarListaClientesATodos() {
        StringBuilder lista = new StringBuilder("Lista de clientes:");
        synchronized (listaClientes) {
            for (AtencionCliente cliente : listaClientes) {
                lista.append(cliente.getNombreCliente()).append(":");
            }
        }
        // Imprimir la lista en la consola
        System.out.println(lista.toString());

        synchronized (listaClientes) {
            for (AtencionCliente cliente : listaClientes) {
                cliente.enviarMensaje(lista.toString());
            }
        }
    }

    public void enviarMensaje(String mensaje) {
        if (out != null) {
            out.println(mensaje);
        } else {
            System.out.println("Error: Output stream is null.");
        }
    }

    private void enviarATodos(String mensaje) {
        for (AtencionCliente cliente : listaClientes) {
            cliente.enviarMensaje("Mensaje del cliente " + this.nombreCliente + ": " + mensaje);
        }
    }

    private void procesarMensaje(String mensaje) {
        // formato del mensaje para un cliente específico es "@nombreCliente: mensaje"
        if (mensaje.startsWith("@")) {
            int separatorIndex = mensaje.indexOf(':');
            if (separatorIndex != -1) {
                String destinatario = mensaje.substring(1, separatorIndex).trim();
                String contenidoMensaje = mensaje.substring(separatorIndex + 1).trim();

                AtencionCliente clienteDestino = buscarClientePorNombre(destinatario);
                if (clienteDestino != null) {
                    clienteDestino.enviarMensaje("Mensaje de " + this.nombreCliente + ": " + contenidoMensaje);
                } else {
                    enviarMensaje("Cliente " + destinatario + " no encontrado.");
                }
            } else {
                enviarMensaje("Formato de mensaje incorrecto. Usa '@nombreCliente: mensaje'.");
            }
        } else {
            enviarATodos(mensaje);
        }
    }

    private AtencionCliente buscarClientePorNombre(String nombre) {
        synchronized (listaClientes) {
            for (AtencionCliente cliente : listaClientes) {
                if (cliente.getNombreCliente().equalsIgnoreCase(nombre)) {
                    return cliente;
                }
            }
        }
        return null;
    }
}
