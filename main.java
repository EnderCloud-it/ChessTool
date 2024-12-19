import javax.swing.*;
import java.awt.*;
import java.io.*;

public class Main {
    private JFrame frame;
    private JTextArea boardDisplay;
    private JTextField moveInput;
    private JLabel bestMoveLabel;
    private Process stockfishProcess;
    private BufferedWriter stockfishWriter;
    private BufferedReader stockfishReader;
    private StringBuilder boardState;

    // Path al motore Stockfish (aggiorna con il percorso corretto sul tuo sistema)
    private static final String STOCKFISH_PATH = "C:\\Users\\Studente\\Downloads\\stockfish-windows-x86-64-avx2\\stockfish\\stockfish-windows-x86-64-avx2.exe";

    public Main() {
        setupGUI();
        initializeStockfish();
        resetBoard();
    }

    private void setupGUI() {
        frame = new JFrame("Analizzatore di Scacchi con Stockfish");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 500);
        frame.setLayout(new BorderLayout());

        boardDisplay = new JTextArea();
        boardDisplay.setEditable(false);
        boardDisplay.setFont(new Font("Courier", Font.PLAIN, 14));
        frame.add(new JScrollPane(boardDisplay), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        moveInput = new JTextField();
        inputPanel.add(moveInput, BorderLayout.CENTER);

        JButton moveButton = new JButton("Esegui mossa");
        moveButton.addActionListener(e -> makeMove());
        inputPanel.add(moveButton, BorderLayout.EAST);

        frame.add(inputPanel, BorderLayout.SOUTH);

        bestMoveLabel = new JLabel("Mossa migliore: Nessuna");
        bestMoveLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(bestMoveLabel, BorderLayout.NORTH);

        frame.setVisible(true);
    }

    private void initializeStockfish() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(STOCKFISH_PATH);
            stockfishProcess = processBuilder.start();

            stockfishWriter = new BufferedWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()));
            stockfishReader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));

            sendCommand("uci");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Errore nell'avvio di Stockfish: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void resetBoard() {
        boardState = new StringBuilder();
        sendCommand("ucinewgame");
        sendCommand("position startpos");
        updateBoardDisplay();
    }

    private void makeMove() {
        String move = moveInput.getText().trim();
        if (move.isEmpty()) {
            return;
        }

        boardState.append(" ").append(move);
        sendCommand("position startpos moves" + boardState);
        sendCommand("go movetime 1000");

        try {
            String line;
            while ((line = stockfishReader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    String bestMove = line.split(" ")[1];
                    bestMoveLabel.setText("Mossa migliore: " + bestMove);
                    break;
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Errore nella comunicazione con Stockfish: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }

        updateBoardDisplay();
        moveInput.setText("");
    }

    private void updateBoardDisplay() {
        sendCommand("d");
        try {
            StringBuilder boardOutput = new StringBuilder();
            String line;
            boolean boardStarted = false;
            while ((line = stockfishReader.readLine()) != null) {
                if (line.startsWith("Legal moves")) {
                    break;
                }
                if (boardStarted) {
                    boardOutput.append(line).append("\n");
                }
                if (line.startsWith(" \"")) {
                    boardStarted = true;
                }
            }
            boardDisplay.setText(boardOutput.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Errore nella lettura della scacchiera: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendCommand(String command) {
        try {
            stockfishWriter.write(command + "\n");
            stockfishWriter.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Errore nell'invio del comando: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeStockfish() {
        sendCommand("quit");
        try {
            stockfishWriter.close();
            stockfishReader.close();
            stockfishProcess.destroy();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Errore nella chiusura di Stockfish: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
