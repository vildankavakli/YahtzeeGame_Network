/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author user
 */
public class ClientHandler implements Runnable {

    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + clientSocket.getInetAddress() + "] Mesaj: " + message);
                out.println("geri gönder: " + message);
            }
        } catch (IOException ex) {
            System.out.println("Baglanti hatasi " + ex.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("İstemci baglantisi kapatildi");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
