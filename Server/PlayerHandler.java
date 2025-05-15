/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

/**
 *
 * @author user
 */
import java.io.*;
import java.net.*;

public class PlayerHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int playerId;

    public PlayerHandler(Socket socket, int playerId) {
        this.socket = socket;
        this.playerId = playerId;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            sendMessage("Hoş geldin, Oyuncu " + playerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String waitForInput() {
        try {
            return in.readLine();  // Oyuncudan mesaj bekleniyor
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            String input;
            while ((input = in.readLine()) != null) {
                System.out.println("Oyuncu " + playerId + ": " + input);

                // İleride burada gelen komutları GameManager'a göndereceğiz.
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Oyuncu " + playerId + " bağlantısı kesildi.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
