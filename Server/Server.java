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
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 2;
    private static List<PlayerHandler> players = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Sunucu başlatılıyor...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Sunucu port " + PORT + " üzerinden dinleniyor.");

            while (players.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Yeni oyuncu bağlandı: " + clientSocket);

                PlayerHandler player = new PlayerHandler(clientSocket, players.size() + 1);
                players.add(player);
                new Thread(player).start();
            }

            System.out.println("İki oyuncu bağlandı. Oyun başlıyor!");

            // Buradan sonra GameManager oyun mantığını başlatabilir.
            // Örn: new GameManager(players).startGame();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
