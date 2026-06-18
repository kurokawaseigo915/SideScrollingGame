
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * シンプルな2D横スクロールゲーム。
 * プレイヤー、プラットフォーム、敵、ゴール地点を含む。
 */
public class SideScrollingGame extends JPanel implements Runnable, KeyListener {
	/** ゲーム内のセルサイズ (1セルのピクセル数) */
	private static final int CELL_SIZE = 20;

    /** ゲームウィンドウの幅 (ピクセル) */
	private static final int WINDOW_WIDTH = 800;

	/** ゲームウィンドウの高さ (ピクセル) */
    private static final int WINDOW_HEIGHT = 400;

    /** プレイヤーの座標 */
    private int playerX, playerY;

    /** プレイヤーのサイズ */
    private int playerWidth = CELL_SIZE;
    private int playerHeight = CELL_SIZE;

    /** 地面のY座標 (マップの高さから計算される) */
    private int groundLevel;

    /** カメラのX座標 (スクロール位置を管理) */
    private int cameraX = 0;

    /** プレイヤーがジャンプ中であるかを示すフラグ */
    private boolean isJumping = false;
    /** プレイヤーが落下中であるかを示すフラグ */
    private boolean isFalling = false;
    /** プレイヤーが左に移動中であるかを示すフラグ */
    private boolean moveLeft = false;
    /** プレイヤーが右に移動中であるかを示すフラグ */
    private boolean moveRight = false;
    /** プレイヤーがゴール地点に到達したかを示すフラグ */
    private boolean reachedGoal = false;
    
    /** プレイヤーのジャンプ速度 */
    private int jumpSpeed = 10;
    /** プレイヤーの落下速度 */
    private int fallSpeed = 5;
    /** 重力加速度 */
    private int gravity = 1;


    /** ゲーム内のすべてのプラットフォームを格納するリスト */
    private ArrayList<Rectangle> platforms;
    /** ゲーム内のすべての床を格納するリスト */
    private ArrayList<Rectangle> floors;
    /** ゲーム内のすべての敵を格納するリスト */
    private ArrayList<Enemy> enemies;
    /** ゴール地点を表す矩形オブジェクト */
    private Rectangle goal;

    /** プレイヤーのスコア (時間経過で増加) */
    private int score = 0;

    /** マップの幅 (セル数) */
    private int mapWidth = 0;
    /** マップの高さ (セル数) */
    private int mapHeight = 0;
    
    /** ゲームスレッド */
    private Thread gameThread;


    /**
     * コンストラクタ。
     * ゲームウィンドウ、マップ、および入力処理を初期化する。
     */
    public SideScrollingGame() {
        addKeyListener(this);
        setFocusable(true);

        platforms = new ArrayList<>();
        floors = new ArrayList<>();
        enemies = new ArrayList<>();

        loadMap("map.txt");
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    /**
     * マップファイルを読み込み、ゲームオブジェクトを初期化する。
     * @param mapFile マップファイルのパス
     */
    private void loadMap(String mapFile) {
        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(mapFile));
            mapHeight = lines.size();
            mapWidth = lines.get(0).length();
            
            int y = 0;
            for (String line : lines) {
                for (int x = 0; x < line.length(); x++) {
                    char cell = line.charAt(x);
                    int pixelX = x * CELL_SIZE;
                    int pixelY = y * CELL_SIZE;

                    switch (cell) {
                    case 'F': // Floor
                        floors.add(new Rectangle(pixelX, pixelY, CELL_SIZE, CELL_SIZE));
                        break;
                    case 'P': // Platform
                        platforms.add(new Rectangle(pixelX, pixelY, CELL_SIZE, CELL_SIZE));
                        break;
                    case 'E': // Enemy
                        enemies.add(new Enemy(pixelX, pixelY, CELL_SIZE, CELL_SIZE, 2));
                        break;
                    case 'S': // Start position
                        playerX = pixelX;
                        playerY = pixelY;
                        break;
                    case 'G': // Goal position
                        goal = new Rectangle(pixelX, pixelY, CELL_SIZE, CELL_SIZE);
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


    /**
     * メインメソッド。ゲームを起動する。
     * @param args コマンドライン引数 (使用しない)
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Game");
        SideScrollingGame game = new SideScrollingGame();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        game.start();
    }

    /**
     * ゲームスレッドを開始する。
     */
    public void start() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    /**
     * ゲームのメインループ。
     * ゲームロジックを更新し、描画する。
     */
    @Override
    public void run() {
        while (true) {
            updateGame();
            repaint();

            if (reachedGoal) {
                break;
            }
            
            try {
                Thread.sleep(16); // 約60FPS（1/60 = 0.016666[sec]）
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        showGoalMessage();
    }

    /**
     * ゲームロジックを更新する。
     * プレイヤーや敵の移動、衝突判定を処理する。
     */
    private void updateGame() {
        if (reachedGoal)
        	return;

        // プレイヤーの左右移動
        if (moveLeft) {
            if (playerX > 0) {
                playerX -= 5;
            } else if (cameraX > 0) {
                cameraX -= 5;
            }
            for (Rectangle platform : platforms) {
                if (checkHorizontalCollision(playerX, playerY, playerWidth, playerHeight, platform)) {
                    playerX += 5; // Undo movement
                    break;
                }
            }
        }
        
        if (moveRight) {
            if (playerX < WINDOW_WIDTH - playerWidth) {
                playerX += 5;
            } else if (cameraX + WINDOW_WIDTH < mapWidth * CELL_SIZE) {
                cameraX += 5;
            }
            for (Rectangle platform : platforms) {
                if (checkHorizontalCollision(playerX, playerY, playerWidth, playerHeight, platform)) {
                    playerX -= 5; // Undo movement
                    break;
                }
            }
        }

        // プレイヤーがマップの右端を超えないよう制御
        if (playerX + cameraX + playerWidth > mapWidth * CELL_SIZE) {
            playerX = mapWidth * CELL_SIZE - cameraX - playerWidth;
        }
        
        // ジャンプと落下の処理
        boolean onPlatform = false;
        for (Rectangle platform : platforms) {
            if (checkVerticalCollision(playerX, playerY + 1, playerWidth, playerHeight, platform)) {
                onPlatform = true;
                break;
            }
        }
        /*
        for (Rectangle floor : floors) {
            if (checkVerticalCollision(playerX, playerY + 1, playerWidth, playerHeight, floor)) {
                onPlatform = true;
                break;
            }
        }
        */
        
        // Jump and fall mechanics
        if (isJumping) {
            playerY -= jumpSpeed;
            jumpSpeed -= gravity;
            if (jumpSpeed <= 0) {
                isJumping = false;
                isFalling = true;
            }
        }

        if (!onPlatform && !isJumping) {
            isFalling = true;
        }

        if (isFalling) {
            playerY += fallSpeed;
            for (Rectangle platform : platforms) {
                if (checkVerticalCollision(playerX, playerY, playerWidth, playerHeight, platform)) {
                    playerY = platform.y - playerHeight;
                    isFalling = false;
                    jumpSpeed = 10; // Reset jump speed
                    break;
                }
            }
            for (Rectangle floor : floors) {
                if (checkVerticalCollision(playerX, playerY, playerWidth, playerHeight, floor)) {
                    playerY = floor.y - playerHeight;
                    isFalling = false;
                    jumpSpeed = 10; // Reset jump speed
                    break;
                }
            }
            if (playerY + playerHeight >= groundLevel) {
                playerY = groundLevel - playerHeight;
                isFalling = false;
                jumpSpeed = 10; // Reset jump speed
            }
        }

        // ゴール地点の判定
        if (goal != null && playerX + playerWidth > goal.x - cameraX &&
            playerX < goal.x + goal.width - cameraX &&
            playerY + playerHeight > goal.y &&
            playerY < goal.y + goal.height) {
            reachedGoal = true;
        }
        
        // 敵の処理
        for (Enemy enemy : enemies) {
            enemy.update(mapWidth);
            for (Rectangle platform : platforms) {
                if (checkVerticalCollision(enemy.x, enemy.y, enemy.width, enemy.height, platform)) {
                    enemy.y = platform.y - enemy.height;
                    break;
                }
                if (checkHorizontalCollision(enemy.x, enemy.y, enemy.width, enemy.height, platform)) {
                    enemy.speed = -enemy.speed; // Reverse direction
                    break;
                }
            }

            for (Rectangle floor : floors) {
                if (checkVerticalCollision(enemy.x, enemy.y, enemy.width, enemy.height, floor)) {
                    enemy.y = floor.y - enemy.height;
                    break;
                }
            }
        }

        // 敵との衝突判定
        for (Enemy enemy : enemies) {
            if (playerX + playerWidth > enemy.x - cameraX &&
                playerX < enemy.x + enemy.width - cameraX &&
                playerY + playerHeight > enemy.y &&
                playerY < enemy.y + enemy.height) {
                System.out.println("Game Over! Final Score: " + score);
                System.exit(0);
            }
        }

        // スコアの更新
        score++;
    }

    /**
     * ゴール到達時のメッセージを表示する。
     */
    private void showGoalMessage() {
        JOptionPane.showMessageDialog(this, "Congratulations! You reached the goal!", "Goal Reached", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }
    
    /**
     * オブジェクトとプラットフォームの水平方向の衝突をチェックする。
     * @param x オブジェクトのX座標
     * @param y オブジェクトのY座標
     * @param width オブジェクトの幅
     * @param height オブジェクトの高さ
     * @param platform プラットフォーム
     * @return 衝突している場合はtrue、そうでない場合はfalse
     */
    private boolean checkHorizontalCollision(int x, int y, int width, int height, Rectangle platform) {
        return x + width > platform.x - cameraX &&
               x < platform.x + platform.width - cameraX &&
               y + height > platform.y &&
               y < platform.y + platform.height;
    }

    
    /**
     * オブジェクトとプラットフォームの垂直方向の衝突をチェックする。
     * @param x オブジェクトのX座標
     * @param y オブジェクトのY座標
     * @param width オブジェクトの幅
     * @param height オブジェクトの高さ
     * @param platform プラットフォーム
     * @return 衝突している場合はtrue、そうでない場合はfalse
     */
    private boolean checkVerticalCollision(int x, int y, int width, int height, Rectangle platform) {
        return x + width > platform.x - cameraX &&
               x < platform.x + platform.width - cameraX &&
               y + height >= platform.y &&
               y < platform.y + platform.height;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background
        g.setColor(new Color(135, 206, 250)); // Sky blue
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw ground level
        g.setColor(new Color(0, 128, 0));	// green
        g.fillRect(0, groundLevel, getWidth(), getHeight() - groundLevel);

        // Draw floors
        g.setColor(new Color(139, 69, 19)); // Brown
        for (Rectangle floor : floors) {
            g.fillRect(floor.x - cameraX, floor.y, floor.width, floor.height);
        }
        
        // Draw platforms
        g.setColor(Color.BLUE);
        for (Rectangle platform : platforms) {
            g.fillRect(platform.x - cameraX, platform.y, platform.width, platform.height);
        }
        
        // Draw goal
        if (goal != null) {
            g.setColor(Color.GREEN);
            g.fillRect(goal.x - cameraX, goal.y, goal.width, goal.height);
        }

        // Draw player
        g.setColor(Color.RED);
        g.fillRect(playerX, playerY, playerWidth, playerHeight);

        // Draw enemies
        g.setColor(Color.BLACK);
        for (Enemy enemy : enemies) {
            g.fillRect(enemy.x - cameraX, enemy.y, enemy.width, enemy.height);
        }

        // Draw score
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);

        // other information
        g.drawString("player = (" + playerX + "," + playerY + ")", 10, 40);
        g.drawString("camera = " + cameraX, 10, 60) ;
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
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    /**
     * 敵キャラクターのクラス。
     * 敵の位置や移動速度を管理する。
     */
    class Enemy {
        int x, y, width, height, speed;

        /**
         * 敵キャラクタのコンストラクタ。
         * @param x 敵の初期X座標
         * @param y 敵の初期Y座標
         * @param width 敵の幅
         * @param height 敵の高さ
         * @param speed 敵の移動速度
         */
        public Enemy(int x, int y, int width, int height, int speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
        }


        /**
         * 敵キャラクタの位置を更新する。
         * @param mapWidth マップ全体の幅
         */
        public void update(int mapWidth) {
            x += speed;
            if (x < 0 || x + width > mapWidth * CELL_SIZE) {
                speed = -speed; // Reverse direction at bounds
            }
        }
    }
}
