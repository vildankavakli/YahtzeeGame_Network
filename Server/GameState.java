/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

/**
 *
 * @author user
 */
import java.util.*;

public class GameState {
    private int currentPlayerIndex;
    private int round;
    private int[][] diceValues; // [playerId][5 zar]
    private Map<Integer, Map<String, Integer>> scoreBoard; // OyuncuId -> (Kategori -> Puan)

    public GameState(int playerCount) {
        currentPlayerIndex = 0;
        round = 1;
        diceValues = new int[playerCount][5];
        scoreBoard = new HashMap<>();

        for (int i = 0; i < playerCount; i++) {
            scoreBoard.put(i, new HashMap<>());
        }
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % scoreBoard.size();
        if (currentPlayerIndex == 0) {
            round++;
        }
    }

    public int getRound() {
        return round;
    }

    public int[] rollDice() {
        Random r = new Random();
        int[] dice = new int[5];
        for (int i = 0; i < 5; i++) {
            dice[i] = r.nextInt(6) + 1;
        }
        diceValues[currentPlayerIndex] = dice;
        return dice;
    }

    public int[] getCurrentPlayerDice() {
        return diceValues[currentPlayerIndex];
    }

    public void setScore(String category, int score) {
        scoreBoard.get(currentPlayerIndex).put(category, score);
    }

    public Map<String, Integer> getScoresForPlayer(int playerId) {
        return scoreBoard.get(playerId);
    }

    public boolean isGameOver() {
        return round > 13; // Yahtzee’de 13 kategori var
    }
}
