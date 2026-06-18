import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class SideScrollingGameAuto extends JPanel implements Runnable, KeyListener {
    private static final int CELL_SIZE = 20;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 400;
    private static final int PLAYER_WIDTH = CELL_SIZE;
    private static final int PLAYER_HEIGHT = CELL_SIZE;
    private static final int ENEMY_SIZE = CELL_SIZE;
    private static final int GROUND_Y = 340;
    private static final int FLOOR_HEIGHT = CELL_SIZE;
    private static final int GENERATE_AHEAD_DISTANCE = 1000;
    private static final int CLEANUP_DISTANCE = 240;
    private static final int RIGHT_EDGE_PADDING = 36;

    private static final double PLAYER_SPEED = 5.0;
    private static final double AUTO_SCROLL_SPEED = 2.2;
    private static final double JUMP_SPEED = 11.0;
    private static final double GRAVITY = 0.55;
    private static final double MAX_FALL_SPEED = 12.0;
    private static final int ENEMY_SPEED = 2;

    private final Random random = new Random();
    private final ArrayList<Rectangle> floors = new ArrayList<>();
    private final ArrayList<Rectangle> platforms = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();

    private double playerX = 80;
    private double playerY = GROUND_Y - PLAYER_HEIGHT;
    private double cameraX = 0;
    private double verticalSpeed = 0;
    private int generatedUntilX = 0;
    private int score = 0;

    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean jumpRequested = false;
    private boolean onGround = true;
    private boolean gameOver = false;
    private boolean lastGeneratedWasGap = false;
    private Thread gameThread;

    public SideScrollingGameAuto() {
        addKeyListener(this);
        setFocusable(true);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        generateStartingArea();
        generateAhead();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Game - Auto Random Mode");
        SideScrollingGameAuto game = new SideScrollingGameAuto();

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
        while (!gameOver) {
            updateGame();
            repaint();

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        repaint();
        JOptionPane.showMessageDialog(this, "Game Over! Final Score: " + score,
                "Auto Random Mode", JOptionPane.WARNING_MESSAGE);
        System.exit(0);
    }

    private void updateGame() {
        cameraX += AUTO_SCROLL_SPEED;
        score = (int) (cameraX / 10);

        generateAhead();
        cleanupOldObjects();
        updatePlayer();
        updateEnemies();
        checkGameOver();
    }

    private void updatePlayer() {
        if (jumpRequested && onGround) {
            verticalSpeed = -JUMP_SPEED;
            onGround = false;
        }
        jumpRequested = false;

        double horizontalSpeed = 0;
        if (moveLeft) {
            horizontalSpeed -= PLAYER_SPEED;
        }
        if (moveRight) {
            horizontalSpeed += PLAYER_SPEED;
        }

        playerX += horizontalSpeed;
        resolveHorizontalCollision(horizontalSpeed);

        double maxPlayerX = cameraX + WINDOW_WIDTH - PLAYER_WIDTH - RIGHT_EDGE_PADDING;
        if (playerX > maxPlayerX) {
            playerX = maxPlayerX;
        }

        verticalSpeed = Math.min(verticalSpeed + GRAVITY, MAX_FALL_SPEED);
        playerY += verticalSpeed;
        resolveVerticalCollision();
    }

    private void resolveHorizontalCollision(double horizontalSpeed) {
        if (horizontalSpeed == 0) {
            return;
        }

        for (Rectangle block : getTerrain()) {
            if (intersectsPlayer(block)) {
                if (horizontalSpeed > 0) {
                    playerX = block.x - PLAYER_WIDTH;
                } else {
                    playerX = block.x + block.width;
                }
            }
        }
    }

    private void resolveVerticalCollision() {
        onGround = false;

        for (Rectangle block : getTerrain()) {
            if (intersectsPlayer(block)) {
                if (verticalSpeed > 0) {
                    playerY = block.y - PLAYER_HEIGHT;
                    onGround = true;
                } else if (verticalSpeed < 0) {
                    playerY = block.y + block.height;
                }
                verticalSpeed = 0;
            }
        }
    }

    private ArrayList<Rectangle> getTerrain() {
        ArrayList<Rectangle> terrain = new ArrayList<>();
        terrain.addAll(floors);
        terrain.addAll(platforms);
        return terrain;
    }

    private boolean intersectsPlayer(Rectangle block) {
        return playerX + PLAYER_WIDTH > block.x &&
               playerX < block.x + block.width &&
               playerY + PLAYER_HEIGHT > block.y &&
               playerY < block.y + block.height;
    }

    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            enemy.update();
            if (intersectsEnemy(enemy)) {
                gameOver = true;
            }
        }
    }

    private boolean intersectsEnemy(Enemy enemy) {
        return playerX + PLAYER_WIDTH > enemy.x &&
               playerX < enemy.x + enemy.width &&
               playerY + PLAYER_HEIGHT > enemy.y &&
               playerY < enemy.y + enemy.height;
    }

    private void checkGameOver() {
        if (playerX + PLAYER_WIDTH < cameraX || playerY > WINDOW_HEIGHT + 80) {
            gameOver = true;
        }
    }

    private void generateStartingArea() {
        int startWidth = 900;
        floors.add(new Rectangle(0, GROUND_Y, startWidth, FLOOR_HEIGHT));
        generatedUntilX = startWidth;
    }

    private void generateAhead() {
        int targetX = (int) cameraX + WINDOW_WIDTH + GENERATE_AHEAD_DISTANCE;

        while (generatedUntilX < targetX) {
            boolean canMakeGap = generatedUntilX > 1100 && !lastGeneratedWasGap;
            boolean makeGap = canMakeGap && random.nextDouble() < 0.24;

            if (makeGap) {
                int gapWidth = randomBetween(2, 4) * CELL_SIZE;
                maybeAddBridgePlatform(generatedUntilX, gapWidth);
                generatedUntilX += gapWidth;
                lastGeneratedWasGap = true;
                continue;
            }

            int segmentWidth = randomBetween(5, 10) * CELL_SIZE;
            Rectangle floor = new Rectangle(generatedUntilX, GROUND_Y, segmentWidth, FLOOR_HEIGHT);
            floors.add(floor);
            maybeAddEnemyOn(floor, 0.38);
            maybeAddPlatformAbove(floor);

            generatedUntilX += segmentWidth;
            lastGeneratedWasGap = false;
        }
    }

    private void maybeAddBridgePlatform(int startX, int gapWidth) {
        if (random.nextDouble() < 0.45) {
            int platformWidth = gapWidth + randomBetween(1, 3) * CELL_SIZE;
            int platformX = startX - CELL_SIZE;
            int platformY = randomPlatformY();
            Rectangle platform = new Rectangle(platformX, platformY, platformWidth, CELL_SIZE);
            platforms.add(platform);
            maybeAddEnemyOn(platform, 0.25);
        }
    }

    private void maybeAddPlatformAbove(Rectangle floor) {
        if (floor.width < CELL_SIZE * 5 || random.nextDouble() > 0.42) {
            return;
        }

        int platformCells = randomBetween(3, 6);
        int platformWidth = Math.min(platformCells * CELL_SIZE, floor.width - CELL_SIZE);
        int freeWidth = Math.max(CELL_SIZE, floor.width - platformWidth);
        int platformX = floor.x + randomBetween(0, freeWidth / CELL_SIZE) * CELL_SIZE;
        int platformY = randomPlatformY();
        Rectangle platform = new Rectangle(platformX, platformY, platformWidth, CELL_SIZE);
        platforms.add(platform);
        maybeAddEnemyOn(platform, 0.28);
    }

    private int randomPlatformY() {
        int[] platformYs = {220, 260, 300};
        return platformYs[random.nextInt(platformYs.length)];
    }

    private void maybeAddEnemyOn(Rectangle block, double probability) {
        if (block.width < CELL_SIZE * 3 || random.nextDouble() > probability) {
            return;
        }

        int enemyX = block.x + randomBetween(1, Math.max(1, block.width / CELL_SIZE - 2)) * CELL_SIZE;
        int minX = block.x;
        int maxX = block.x + block.width;
        int speed = random.nextBoolean() ? ENEMY_SPEED : -ENEMY_SPEED;
        enemies.add(new Enemy(enemyX, block.y - ENEMY_SIZE, ENEMY_SIZE, ENEMY_SIZE, speed, minX, maxX));
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private void cleanupOldObjects() {
        int cleanupX = (int) cameraX - CLEANUP_DISTANCE;

        removeOldRectangles(floors, cleanupX);
        removeOldRectangles(platforms, cleanupX);

        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (enemy.x + enemy.width < cleanupX) {
                enemyIterator.remove();
            }
        }
    }

    private void removeOldRectangles(ArrayList<Rectangle> rectangles, int cleanupX) {
        Iterator<Rectangle> iterator = rectangles.iterator();
        while (iterator.hasNext()) {
            Rectangle rectangle = iterator.next();
            if (rectangle.x + rectangle.width < cleanupX) {
                iterator.remove();
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int cameraInt = (int) cameraX;
        g.setColor(new Color(105, 190, 230));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(new Color(42, 120, 75));
        g.fillRect(0, GROUND_Y + FLOOR_HEIGHT, getWidth(), getHeight() - GROUND_Y - FLOOR_HEIGHT);

        g.setColor(new Color(132, 82, 44));
        for (Rectangle floor : floors) {
            g.fillRect(floor.x - cameraInt, floor.y, floor.width, floor.height);
        }

        g.setColor(new Color(35, 90, 205));
        for (Rectangle platform : platforms) {
            g.fillRect(platform.x - cameraInt, platform.y, platform.width, platform.height);
        }

        g.setColor(new Color(98, 0, 20));
        for (Enemy enemy : enemies) {
            g.fillRect((int) enemy.x - cameraInt, (int) enemy.y, enemy.width, enemy.height);
        }

        g.setColor(new Color(225, 35, 35));
        g.fillRect((int) playerX - cameraInt, (int) playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
        g.drawString("AUTO RANDOM MODE", WINDOW_WIDTH - 150, 20);

        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            g.setColor(Color.WHITE);
            g.drawString("GAME OVER", WINDOW_WIDTH / 2 - 35, WINDOW_HEIGHT / 2);
        }
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
                jumpRequested = true;
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
        double x, y;
        int width, height, speed, minX, maxX;

        Enemy(double x, double y, int width, int height, int speed, int minX, int maxX) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
            this.minX = minX;
            this.maxX = maxX;
        }

        void update() {
            x += speed;
            if (x < minX) {
                x = minX;
                speed = -speed;
            } else if (x + width > maxX) {
                x = maxX - width;
                speed = -speed;
            }
        }
    }
}
