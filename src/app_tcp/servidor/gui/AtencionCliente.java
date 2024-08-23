package app_tcp.servidor.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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

                if (linea.equals("enviar clientes activos")) {
                    // Solicitar clientes activos
                    this.enviarListaClientesATodos();
                }

//                if (linea.contains("desconecto")) {
//                     Actualizar la lista con el cliente eliminado
//                    this.cerrarSesion(linea);
//                }

                mensajesTxt.append("Cliente: " + this.nombreCliente + "\n");
                System.out.println("Mensaje: " + linea + "\n");
                //Ejercicio dos
                //this.procesarMensaje(linea);

                //Ejercicio Tres
                procesarMensajeDeArchivo(linea);
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

    private void cerrarSesion(String mensaje) {
        String nombreCliente = mensaje.substring(mensaje.indexOf(":") + 1).trim();
        AtencionCliente cliente = buscarClientePorNombre(nombreCliente);
        
        synchronized (listaClientes) {
            listaClientes.remove(cliente);
        }
        this.enviarListaClientesATodos();
        mensajesTxt.append("Cliente " + cliente.getNombreCliente() + " se ha desconectado.\n");
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
        if (mensaje.startsWith("@")) {
            procesarMensajePrivado(mensaje);
        } else {
            enviarATodos(mensaje);
        }
    }

    private void procesarMensajeDeArchivo(String mensaje) {
        String[] partes = mensaje.split("@");
        if (partes.length == 2) {
            String infoArchivo = partes[0];
            String destinatario = partes[1].trim();

            AtencionCliente clienteDestino = buscarClientePorNombre(destinatario);
            if (clienteDestino != null) {
                recibirArchivo(mensaje, clienteDestino);
            } else {
                enviarMensaje("Cliente " + destinatario + " no encontrado.");
            }
        }
    }

    private void procesarMensajePrivado(String mensaje) {
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

    private void recibirArchivo(String mensaje, AtencionCliente clienteDestino) {
        try {
            // Extraer el nombre del archivo del mensaje
            String nombreArchivo = mensaje.substring(6); // Ajusta el índice según tu formato
            clienteDestino.enviarMensaje("Archivo recibido de " + this.nombreCliente);

            // Mostrar un mensaje en el cliente destino
            int opcion = JOptionPane.showConfirmDialog(null, "Has recibido un archivo del cliente " + this.nombreCliente + ": " + nombreArchivo + ". ¿Deseas descargarlo?", "Archivo Recibido", JOptionPane.YES_NO_OPTION);
            if (opcion == JOptionPane.YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(nombreArchivo));
                int seleccion = fileChooser.showSaveDialog(null);

                if (seleccion == JFileChooser.APPROVE_OPTION) {
                    File archivoDestino = fileChooser.getSelectedFile();
                    guardarArchivo(archivoDestino, clienteDestino);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void guardarArchivo(File archivoDestino, AtencionCliente clienteDestino) {
        try (InputStream in = clienteDestino.clientSocket.getInputStream(); FileOutputStream fos = new FileOutputStream(archivoDestino)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar el archivo: " + e.getMessage());
        }
    }

}
