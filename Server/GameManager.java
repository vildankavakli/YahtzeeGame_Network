/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

/**
 *
 * @author user
 */
import java.util.List;

public class GameManager {
    private List<PlayerHandler> players;
    private GameState gameState;

    public GameManager(List<PlayerHandler> players) {
        this.players = players;
        this.gameState = new GameState(players.size());
    }

    public void startGame() {
        while (!gameState.isGameOver()) {
            int current = gameState.getCurrentPlayerIndex();
            PlayerHandler player = players.get(current);

            player.sendMessage("Sıra sende! Zar atmak için 'roll' yaz.");

            String input = player.waitForInput(); // input bekle
            if (input.equalsIgnoreCase("roll")) {
                int[] dice = gameState.rollDice();
                player.sendMessage("Zarlar: " + arrayToString(dice));

                player.sendMessage("Kategori seç: örn. 'full_house', 'yahtzee' vs.");
                String category = player.waitForInput();

                int score = ScoreCalculator.calculate(category, dice);
                gameState.setScore(category, score);
                player.sendMessage("Kategoriye " + score + " puan yazıldı.");
            }

            gameState.nextTurn();
        }

        broadcast("Oyun bitti! Sonuçlar:");
        for (int i = 0; i < players.size(); i++) {
            players.get(i).sendMessage("Oyuncu " + (i+1) + ": " + gameState.getScoresForPlayer(i));
        }
    }

    private void broadcast(String message) {
        for (PlayerHandler p : players) {
            p.sendMessage(message);
        }
    }

    private String arrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i : arr) {
            sb.append(i).append(" ");
        }
        return sb.toString().trim();
    }
}
