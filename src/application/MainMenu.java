package application;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MainMenu extends StackPane {

    // palette
    static final Color WHITE      = Color.rgb(0xF0, 0xEB, 0xD8);
    static final Color OFF_WHITE  = Color.rgb(0xD4, 0xCF, 0xBC);
    static final Color DIM_WHITE  = Color.rgb(0x90, 0x8C, 0x80);
    static final Color BLACK      = Color.rgb(0x08, 0x08, 0x08);
    static final Color ACCENT     = Color.rgb(0xE8, 0xE0, 0xC0);

    // layout (Kept 700x400 from HEAD so it perfectly matches the GameBoard)
    static final int W = 700, H = 400;

    // title region
    static final int TITLE_X  = 45;
    static final int TITLE_Y  = 180;

    // menu layout
    static final int MENU_X   = 55;
    static final int MENU_Y0  = 250;
    static final int MENU_GAP = 30;

    static final String[] BTN_LABELS = {
        "NEW GAME", "MULTIPLAYER", "HOW TO PLAY", "SETTINGS", "EXIT"
    };

    // button hitboxes y-coords
    double[] btnY = new double[BTN_LABELS.length];

    // state
    Image bg;
    int tick = 0;
    int hoveredBtn = -1;
    int scanLine = 0;

    // ambient sparks
    ArrayList<Spark> sparks = new ArrayList<>();
    Random rnd = new Random();

    Runnable onStart;
    Runnable onMultiplayer; // Kept from HEAD
    Runnable onExit;

    // font sizes
    static final int TITLE_FONT_SIZE = 72;
    static final int MENU_FONT_SIZE  = 22;

    public MainMenu(Runnable onStart, Runnable onMultiplayer, Runnable onExit) {
        this.onStart = onStart;
        this.onMultiplayer = onMultiplayer;
        this.onExit = onExit;

        this.setAlignment(Pos.CENTER);
        
        Canvas canvas = new Canvas(W, H);
        this.getChildren().add(canvas);

        // background setup

        try {
            InputStream is = getClass().getResourceAsStream("/screens/menu_background.png");
            if (is != null) {
                bg = new Image(is);
            } else {
                System.out.println("menu_background.png not found in resources!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // hitboxes setup
        for (int i = 0; i < BTN_LABELS.length; i++) {
            btnY[i] = MENU_Y0 + i * MENU_GAP;
        }

        // mouse interactions
        canvas.setOnMouseMoved(e -> {
            hoveredBtn = -1;
            for (int i = 0; i < BTN_LABELS.length; i++) {
                if (e.getX() >= MENU_X - 20 && e.getX() <= MENU_X + 250 &&
                    e.getY() >= btnY[i] - MENU_FONT_SIZE && e.getY() <= btnY[i] + 10) {
                    hoveredBtn = i;
                    break;
                }
            }
        });

        canvas.setOnMouseClicked(e -> {
            if (hoveredBtn != -1) {
                handleBtn(hoveredBtn);
            }
        });

        // main loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tick++;
                scanLine = (scanLine + 2) % H;

                // spawn sparks
                sparks.removeIf(s -> !s.alive());
                for (Spark s : sparks) s.update();

                render(canvas.getGraphicsContext2D());
            }
        };
        timer.start();
    }

    void handleBtn(int i) {
        switch (i) {
            case 0 -> onStart.run();
            case 1 -> onMultiplayer.run(); // From HEAD
            case 2 -> {
                // Show How To Play as an overlay (From feat/shamel)
                HowToPlayScreen overlay = new HowToPlayScreen(() -> {
                    this.getChildren().remove(this.getChildren().size() - 1); // remove the overlay
                });
                this.getChildren().add(overlay);
            }
            case 3 -> {
                // Show Settings as an overlay (From feat/shamel)
                SettingsScreen overlay = new SettingsScreen(() -> {
                    this.getChildren().remove(this.getChildren().size() - 1); // remove the overlay
                });
                this.getChildren().add(overlay);
            }
            case 4 -> onExit.run();
            default -> System.out.println("Clicked " + BTN_LABELS[i] + " ... NotImplemented");
        }
    }

    void emitAmbientSparks(int n) {
        for (int i = 0; i < n; i++) {
            int sx = rnd.nextInt(W);
            int sy = H - rnd.nextInt(80);
            float angle = (float)(-Math.PI / 2 + (rnd.nextFloat() - 0.5f) * 1.2f);
            float speed = 0.6f + rnd.nextFloat() * 1.4f;
            Color col = Color.rgb(200 + rnd.nextInt(55), 180 + rnd.nextInt(60), 60 + rnd.nextInt(60));
            sparks.add(new Spark(sx, sy, angle, speed, col));
        }
    }

    // render graphics
    void render(GraphicsContext g) {
        // background
        if (bg != null && !bg.isError()) {
            g.drawImage(bg, 0, 0, W, H);
        } else {
            g.setFill(Color.rgb(0x12, 0x1E, 0x12));
            g.fillRect(0, 0, W, H);
        }

        // side vignette
        LinearGradient leftFade = new LinearGradient(
                0, 0, 420, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0, 0, 0, 0.78)),
                new Stop(1, Color.color(0, 0, 0, 0))
        );
        g.setFill(leftFade);
        g.fillRect(0, 0, W, H);

        // bottom vignette
        LinearGradient bottomFade = new LinearGradient(
                0, H - 80, 0, H, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0, 0, 0, 0)),
                new Stop(1, Color.color(0, 0, 0, 0.62))
        );
        g.setFill(bottomFade);
        g.fillRect(0, H - 80, W, 80);

        // scanlines
        g.setFill(Color.rgb(0, 0, 0, 0.086));
        for (int y = 0; y < H; y += 3) g.fillRect(0, y, W, 1);
        g.setFill(Color.rgb(255, 255, 255, 0.03));
        g.fillRect(0, scanLine, W, 2);

        // sparks
        for (Spark s : new ArrayList<>(sparks)) s.draw(g);

        // texts
        drawTitle(g);
        drawMenuItems(g);
        // footer
        drawFooter(g);
    }

    // draw title
    void drawTitle(GraphicsContext g) {
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, TITLE_FONT_SIZE));
        
        String line1 = "PASS";
        String line2 = "THE BOMB";
        int lineH = TITLE_FONT_SIZE + 4;

        // text shadow
        g.setFill(Color.color(0, 0, 0, 0.7));
        g.fillText(line1, TITLE_X + 4, TITLE_Y - lineH + 4);
        g.fillText(line2, TITLE_X + 4, TITLE_Y + 4);

        // text outline
        g.setFill(BLACK);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx == 0 && dy == 0) continue;
                g.fillText(line1, TITLE_X + dx, TITLE_Y - lineH + dy);
                g.fillText(line2, TITLE_X + dx, TITLE_Y + dy);
            }
        }

        // main text
        g.setFill(WHITE);
        g.fillText(line1, TITLE_X, TITLE_Y - lineH);
        g.fillText(line2, TITLE_X, TITLE_Y);
    }

    // draw menu items
    void drawMenuItems(GraphicsContext g) {
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, MENU_FONT_SIZE));

        for (int i = 0; i < BTN_LABELS.length; i++) {
            String label = BTN_LABELS[i];
            boolean hov = (i == hoveredBtn);

            int tx = MENU_X;
            int ty = MENU_Y0 + i * MENU_GAP;

            if (hov) {
                // hovered text
                tx += 8;

                g.setFill(Color.color(0, 0, 0, 0.62));
                g.fillText(label, tx + 2, ty + 2);

                g.setFill(ACCENT);
                g.fillText(label, tx, ty);

                // triangle marker
                int triX = MENU_X - 12;
                int triY = ty - MENU_FONT_SIZE / 2 + 3;
                double[] px = { triX, triX, triX + 8 };
                double[] py = { triY - 6, triY + 6, triY };
                g.setFill(WHITE);
                g.fillPolygon(px, py, 3);
            } else {
                // normal text
                g.setFill(Color.color(0, 0, 0, 0.47));
                g.fillText(label, tx + 2, ty + 2);

                g.setFill(DIM_WHITE);
                g.fillText(label, tx, ty);
            }
        }
    }

    void drawFooter(GraphicsContext g) {
        g.setFont(Font.font("Monospaced", FontWeight.NORMAL, 11));
        String txt = "\u00a9 2025  PASS THE BOMB  |  UPLB EDITION";
        
        g.setFill(Color.color(1, 1, 1, 0.27));
        double textW = txt.length() * 6.5; 
        g.fillText(txt, (W - textW) / 2, H - 10);
    }

    static class Spark {
        float x, y, vx, vy, life;
        Color color;
        int size;

        Spark(int x, int y, float angle, float speed, Color col) {
            this.x = x; this.y = y;
            this.vx = (float)(Math.cos(angle) * speed);
            this.vy = (float)(Math.sin(angle) * speed);
            this.life = 1f;
            this.color = col;
            this.size = 1 + (int)(Math.random() * 2);
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.05f;
            vx *= 0.98f;
            life -= 0.022f;
        }

        boolean alive() { return life > 0; }

        void draw(GraphicsContext g) {
            double a = Math.max(0, Math.min(1.0, life * 0.78));
            g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), a));
            g.fillRect(x - size / 2.0, y - size / 2.0, size, size);
        }
    }
}