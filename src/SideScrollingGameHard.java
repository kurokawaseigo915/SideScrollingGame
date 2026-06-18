import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class SideScrollingGameHard extends JPanel implements Runnable, KeyListener {
    private static final int CELL_SIZE = 20;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 400;
    private static final int PLAYER_SPEED = 5;
    private static final int INITIAL_JUMP_SPEED = 10;
    private static final int FALL_SPEED = 5;
    private static final int GRAVITY = 1;
    private static final int ENEMY_SPEED = 3;
    private static final String MAP_FILE = "map_hard.txt";

    private int playerX, playerY;
    private int playerWidth = CELL_SIZE;
    private int playerHeight = CELL_SIZE;
    private int groundLevel;
    private int cameraX = 0;

    private boolean isJumping = false;
    private boolean isFalling = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean reachedGoal = false;

    private int jumpSpeed = INITIAL_JUMP_SPEED;

    private ArrayList<Rectangle> platforms;
    private ArrayList<Rectangle> floors;
    private ArrayList<Enemy> enemies;
    private Rectangle goal;

    private int score = 0;
    private int mapWidth = 0;
    private int mapHeight = 0;
    private Thread gameThread;

    public SideScrollingGameHard() {
        addKeyListener(this);
        setFocusable(true);

        platforms = new ArrayList<>();
        floors = new ArrayList<>();
        enemies = new ArrayList<>();

        loadMap(MAP_FILE);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    private void loadMap(String mapFile) {
        try {
            java.util.List<String> lines = readMapLines(mapFile);
            mapHeight = lines.size();
            mapWidth = lines.get(0).length();

            int y = 0;
            for (String line : lines) {
                for (int x = 0; x < line.length(); x++) {
                    char cell = line.charAt(x);
                    int pixelX = x * CELL_SIZE;
                    int pixelY = y * CELL_SIZE;

                    switch (cell) {
                    case 'F':
                        floors.add(new Rectangle(pixelX, pixelY, CELL_SIZE, CELL_SIZE));
                        break;
                    case 'P':
                        platforms.add(new Rectangle(pixelX, pixelY, CELL_SIZE, CELL_SIZE));
                        break;
                    case 'E':
                        enemies.add(new Enemy(pixelX, pixelY, CELL_SIZE, CELL_SIZE, ENEMY_SPEED));
                        break;
                    case 'S':
                        playerX = pixelX;
                        playerY = pixelY;
                        break;
                    case 'G':
                        goal = new Rectangle(pixelX, pixelY, CELL_SIZE, CELL_SIZE);
                        break;
                    default:
                        break;
                    }
                }
                y++;
            }
            groundLevel = mapHeight * CELL_SIZE;
        } catch (IOException e) {
            System.err.println("Error loading map: " + e.getMessage());
        }
    }

    private java.util.List<String> readMapLines(String mapFile) throws IOException {
        Path mapPath = Paths.get(mapFile);
        if (Files.exists(mapPath)) {
            return Files.readAllLines(mapPath);
        }

        InputStream input = SideScrollingGameHard.class.getResourceAsStream("/" + mapFile);
        if (input == null) {
            throw new IOException("Map file not found: " + mapFile);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            java.util.List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Game - Hard Mode");
        SideScrollingGameHard game = new SideScrollingGameHard();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        game.start();
    }

    public void start() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (true) {
            updateGame();
            repaint();

            if (reachedGoal) {
                break;
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        showGoalMessage();
    }

    private void updateGame() {
        if (reachedGoal) {
            return;
        }

        if (moveLeft) {
            movePlayerHorizontal(-PLAYER_SPEED);
        }

        if (moveRight) {
            movePlayerHorizontal(PLAYER_SPEED);
        }

        if (playerX + cameraX + playerWidth > mapWidth * CELL_SIZE) {
            playerX = mapWidth * CELL_SIZE - cameraX - playerWidth;
        }

        boolean onSurface = isStandingOn(platforms) || isStandingOn(floors);

        if (isJumping) {
            playerY -= jumpSpeed;
            jumpSpeed -= GRAVITY;
            if (jumpSpeed <= 0) {
                isJumping = false;
                isFalling = true;
            }
        }

        if (!onSurface && !isJumping) {
            isFalling = true;
        }

        if (isFalling) {
            playerY += FALL_SPEED;
            landOn(platforms);
            landOn(floors);

            if (playerY + playerHeight >= groundLevel) {
                playerY = groundLevel - playerHeight;
                isFalling = false;
                jumpSpeed = INITIAL_JUMP_SPEED;
            }
        }

        if (goal != null && intersectsScreenWithWorld(playerX, playerY, playerWidth, playerHeight, goal)) {
            reachedGoal = true;
        }

        for (Enemy enemy : enemies) {
            enemy.update(mapWidth);

            for (Rectangle platform : platforms) {
                if (intersectsWorld(enemy.x, enemy.y, enemy.width, enemy.height, platform)) {
                    if (enemy.y + enemy.height <= platform.y + FALL_SPEED) {
                        enemy.y = platform.y - enemy.height;
                    } else {
                        enemy.speed = -enemy.speed;
                    }
                    break;
                }
            }

            for (Rectangle floor : floors) {
                if (intersectsWorld(enemy.x, enemy.y, enemy.width, enemy.height, floor)) {
                    enemy.y = floor.y - enemy.height;
                    break;
                }
            }
        }

        for (Enemy enemy : enemies) {
            if (intersectsScreenWithWorld(playerX, playerY, playerWidth, playerHeight,
                    new Rectangle(enemy.x, enemy.y, enemy.width, enemy.height))) {
                JOptionPane.showMessageDialog(this, "Game Over! Final Score: " + score,
                        "Hard Mode", JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
        }

        score++;
    }

    private void movePlayerHorizontal(int dx) {
        int oldPlayerX = playerX;
        int oldCameraX = cameraX;
        int mapPixelWidth = mapWidth * CELL_SIZE;
        int maxCameraX = Math.max(0, mapPixelWidth - WINDOW_WIDTH);

        if (dx < 0) {
            if (playerX > 0) {
                playerX = Math.max(0, playerX + dx);
            } else if (cameraX > 0) {
                cameraX = Math.max(0, cameraX + dx);
            }
        } else if (dx > 0) {
            if (playerX < WINDOW_WIDTH - playerWidth) {
                playerX = Math.min(WINDOW_WIDTH - playerWidth, playerX + dx);
            } else if (cameraX < maxCameraX) {
                cameraX = Math.min(maxCameraX, cameraX + dx);
            }
        }

        if (collidesWithTerrain()) {
            playerX = oldPlayerX;
            cameraX = oldCameraX;
        }
    }

    private boolean isStandingOn(ArrayList<Rectangle> blocks) {
        for (Rectangle block : blocks) {
            if (intersectsScreenWithWorld(playerX, playerY + 1, playerWidth, playerHeight, block)) {
                return true;
            }
        }
        return false;
    }

    private void landOn(ArrayList<Rectangle> blocks) {
        for (Rectangle block : blocks) {
            if (intersectsScreenWithWorld(playerX, playerY, playerWidth, playerHeight, block)) {
                playerY = block.y - playerHeight;
                isFalling = false;
                jumpSpeed = INITIAL_JUMP_SPEED;
                break;
            }
        }
    }

    private boolean collidesWithAny(ArrayList<Rectangle> blocks) {
        for (Rectangle block : blocks) {
            if (intersectsScreenWithWorld(playerX, playerY, playerWidth, playerHeight, block)) {
                return true;
            }
        }
        return false;
    }

    private boolean collidesWithTerrain() {
        return collidesWithAny(platforms) || collidesWithAny(floors);
    }

    private boolean intersectsScreenWithWorld(int screenX, int y, int width, int height, Rectangle worldRect) {
        int worldX = screenX + cameraX;
        return worldX + width > worldRect.x &&
               worldX < worldRect.x + worldRect.width &&
               y + height > worldRect.y &&
               y < worldRect.y + worldRect.height;
    }

    private boolean intersectsWorld(int x, int y, int width, int height, Rectangle worldRect) {
        return x + width > worldRect.x &&
               x < worldRect.x + worldRect.width &&
               y + height > worldRect.y &&
               y < worldRect.y + worldRect.height;
    }

    private void showGoalMessage() {
        JOptionPane.showMessageDialog(this, "Congratulations! You cleared Hard Mode!",
                "Hard Mode Cleared", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(new Color(95, 175, 220));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(new Color(0, 110, 55));
        g.fillRect(0, groundLevel, getWidth(), getHeight() - groundLevel);

        g.setColor(new Color(125, 66, 28));
        for (Rectangle floor : floors) {
            g.fillRect(floor.x - cameraX, floor.y, floor.width, floor.height);
        }

        g.setColor(new Color(30, 80, 210));
        for (Rectangle platform : platforms) {
            g.fillRect(platform.x - cameraX, platform.y, platform.width, platform.height);
        }

        if (goal != null) {
            g.setColor(new Color(255, 210, 0));
            g.fillRect(goal.x - cameraX, goal.y, goal.width, goal.height);
        }

        g.setColor(new Color(220, 40, 40));
        g.fillRect(playerX, playerY, playerWidth, playerHeight);

        g.setColor(new Color(90, 0, 0));
        for (Enemy enemy : enemies) {
            g.fillRect(enemy.x - cameraX, enemy.y, enemy.width, enemy.height);
        }

        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
        g.drawString("player = (" + playerX + "," + playerY + ")", 10, 40);
        g.drawString("camera = " + cameraX, 10, 60);

        g.setColor(new Color(255, 235, 90));
        g.drawString("HARD MODE", WINDOW_WIDTH - 100, 20);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                moveLeft = true;
                break;
            case KeyEvent.VK_RIGHT:
                moveRight = true;
                break;
            case KeyEvent.VK_SPACE:
                if (!isJumping && !isFalling) {
                    isJumping = true;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                moveLeft = false;
                break;
            case KeyEvent.VK_RIGHT:
                moveRight = false;
                break;
            default:
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    class Enemy {
        int x, y, width, height, speed;

        public Enemy(int x, int y, int width, int height, int speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
        }

        public void update(int mapWidth) {
            x += speed;
            if (x < 0 || x + width > mapWidth * CELL_SIZE) {
                speed = -speed;
            }
        }
    }
}
