/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Client;

/**
 *
 * @author user
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class GameClientGUI extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton rollButton;
    private JComboBox<String> categoryBox;
    private PrintWriter out;

    public GameClientGUI(String serverIP, int port) {
        setTitle("Yahtzee - Oyuncu");
        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(messageArea);

        inputField = new JTextField();
        sendButton = new JButton("Gönder");
        rollButton = new JButton("Zar At");

        categoryBox = new JComboBox<>(new String[]{
                "chance", "yahtzee", "full_house"
                // Diğer kategoriler sonra eklenebilir
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        topPanel.add(rollButton);
        topPanel.add(categoryBox);

        add(scroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        setVisible(true);

        // Socket bağlantısı
        new Thread(() -> connectToServer(serverIP, port)).start();

        // Event Listeners
        sendButton.addActionListener(e -> {
            String text = inputField.getText();
            if (!text.isEmpty()) {
                out.println(text);
                inputField.setText("");
            }
        });

        rollButton.addActionListener(e -> {
            out.println("roll");
        });

        categoryBox.addActionListener(e -> {
            String category = (String) categoryBox.getSelectedItem();
            if (category != null) {
                out.println(category);
            }
        });
    }

    private void connectToServer(String serverIP, int port) {
        try (Socket socket = new Socket(serverIP, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                appendMessage(message);
            }

        } catch (IOException e) {
            appendMessage("Bağlantı hatası: " + e.getMessage());
        }
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        String serverIP = JOptionPane.showInputDialog("Server IP adresini giriniz:");
        new GameClientGUI(serverIP, 12345);
    }
}
