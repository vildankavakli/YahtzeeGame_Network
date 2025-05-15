/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Client;

/**
 *
 * @author user
 */
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        String serverIP = "AWS_SERVER_IP"; // Buraya AWS sunucu IP’si gelecek
        int port = 12345;

        try (Socket socket = new Socket(serverIP, port); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); Scanner scanner = new Scanner(System.in)) {

            System.out.println("Server’a bağlandı. Mesajlar:");

            // Server'dan gelen mesajları dinleyen thread
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println("[SERVER] " + msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Kullanıcıdan giriş alıp server’a gönder
            while (true) {
                String input = scanner.nextLine();
                out.println(input);
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
