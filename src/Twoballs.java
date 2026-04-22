import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Joachim Parrow 2010 rev 2011, 2012, 2013, 2015, 2016
 *
 * Simulator for two balls
 */


public class Twoballs {

    final static int UPDATE_FREQUENCY = 100;    // Global constant: fps, ie times per second to simulate

    public static void main(String[] args) {

        JFrame frame = new JFrame("Perfect Collisions");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 2));

        JLabel player1 = new JLabel("Player 1 score: ");
        player1.setFont(new Font("Arial", Font.BOLD, 18));
        JLabel player2 = new JLabel("Player 2 score: ");
        player2.setFont(new Font("Arial", Font.BOLD, 18));

        controlPanel.add(player1);
        controlPanel.add(player2);

        Table table = new Table(player1, player2);

        JButton resetButton = new JButton("Restart");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.restartGame();
            }
        });

        frame.add(resetButton, BorderLayout.SOUTH);
        frame.add(table, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);
    }
}

/**
 * *****************************************************************************************
 * Coord
 *
 * A coordinate is a pair (x,y) of doubles. Also used to represent vectors. Here
 * are various utility methods to compute with vectors.
 *
 *
 */
class Coord {

    double x, y;

    Coord(double xCoord, double yCoord) {
        x = xCoord;
        y = yCoord;
    }
    
    Coord(MouseEvent event) {                   // Create a Coord from a mouse event
        x = event.getX();
        y = event.getY();
    }

    static final Coord ZERO = new Coord(0,0);
    
    double magnitude() {                        
        return Math.sqrt(x * x + y * y);
    }

    Coord norm() {                              // norm: a normalised vector at the same direction
        return new Coord(x / magnitude(), y / magnitude());
    }

    Coord normBetweenTwoCoords(Coord firstCoord, Coord secondCoord) {
        double distanceBetweenBalls;
        double yDiff = firstCoord.y - secondCoord.y;
        double xDiff = firstCoord.x - secondCoord.x;
        distanceBetweenBalls = Math.sqrt(yDiff*yDiff + xDiff*xDiff);

        double normedX = xDiff / distanceBetweenBalls;
        double normedY = yDiff / distanceBetweenBalls;

        return new Coord(normedX, normedY);
    }


    void increase(Coord c) {           
        x += c.x;
        y += c.y;
    }
    
    void decrease(Coord c) {
        x -= c.x;
        y -= c.y;
    }
    
    static double scal(Coord a, Coord b) {      // scalar product
        return a.x * b.x + a.y * b.y;
    } 
    
    static Coord sub(Coord a, Coord b) {        
        return new Coord(a.x - b.x, a.y - b.y);
    }

    static Coord mul(double k, Coord c) {       // multiplication by a constant
        return new Coord(k * c.x, k * c.y);
    }

    static double distance(Coord a, Coord b) {
        return Coord.sub(a, b).magnitude();
    }

    static void paintLine(Graphics2D graph2D, Coord a, Coord b){  // paint line between points
        graph2D.setColor(Color.black);
        graph2D.drawLine((int)a.x, (int)a.y, (int)b.x, (int)b.y);
    }
}

/**
 * ****************************************************************************************
 * Table
 *
 * The table has some constants and instance variables relating to the graphics and
 * the balls. When simulating the balls it starts a timer
 * which fires UPDATE_FREQUENCY times per second. Each time the timer is
 * activated one step of the simulation is performed. The table reacts to
 * events to accomplish repaints and to stop or start the timer.
 *
 */
class Table extends JPanel implements MouseListener, MouseMotionListener, ActionListener {

    private final int   TABLE_WIDTH    = 300;
    private final int   TABLE_HEIGHT   = 500;
    private final int   WALL_THICKNESS = 20;
    private final Color COLOR          = Color.green;
    private final Color WALL_COLOR     = Color.black;
    private final Timer simulationTimer;
    private final int   NUMBER_OF_BALLS = 10;
    private final List<Ball>  BALL_ARRAY = new ArrayList<>();
    private final List<Hole> HOLE_ARRAY = new ArrayList<>();
    private WhiteBall whiteBall;
    private boolean whiteInHole = false;
    protected boolean isPlayer1Turn = true;
    private JLabel player1Label, player2Label;
    protected int player1Score = 0;
    protected int player2Score = 0;
    private boolean isShooting = false;
    protected boolean ABallWasPocketedThisTurn = false;
    protected boolean placedWhiteBallThisTurn = false;
    
    Table(JLabel player1, JLabel player2) {
        this.player1Label = player1;
        this.player2Label = player2;
        updateScore();

        setPreferredSize(new Dimension(TABLE_WIDTH + 2 * WALL_THICKNESS,
                                       TABLE_HEIGHT + 2 * WALL_THICKNESS));
        createInitialBalls();
        placeBalls();

        createHoles();
        
        addMouseListener(this);
        addMouseMotionListener(this);

        simulationTimer = new Timer((int) (1000.0 / Twoballs.UPDATE_FREQUENCY), this);
        simulationTimer.start();
    }
    public void actionPerformed(ActionEvent e) {          // Timer event
        for (Ball ball: BALL_ARRAY){
            ball.move();
        }
        if (!whiteInHole){
            checkBallCollisions();
            checkHoleCollisions();
            removeBallsOutOfPlay();
        }

        if (isShooting && !isAnyBallMoving()){
            isShooting = false;

            if (whiteInHole || !ABallWasPocketedThisTurn) {
                isPlayer1Turn = !isPlayer1Turn;
            }

            ABallWasPocketedThisTurn = false;
            updateScore();
        }
        repaint();
    }

    public  void restartGame() {
        simulationTimer.stop();

        player1Score = 0;
        player2Score = 0;
        isPlayer1Turn = true;
        whiteInHole = false;
        isShooting = false;
        ABallWasPocketedThisTurn = false;
        BALL_ARRAY.clear();

        createInitialBalls();
        placeBalls();

        updateScore();
        repaint();
        simulationTimer.start();

    }
    public void updateScore() {
        player1Label.setText("Player 1 score: " + player1Score);
        player2Label.setText("Player 2 score: " + player2Score);

        Color activeColor = new Color(0, 0, 0, 255);    // Helt svart
        Color inactiveColor = new Color(0, 0, 0, 100);  // Nedtonad svart (genomskinlig)

        if (isPlayer1Turn) {
            player1Label.setForeground(activeColor);
            player2Label.setForeground(inactiveColor);
        } else {
            player1Label.setForeground(inactiveColor);
            player2Label.setForeground(activeColor);
        }
    }
    public int getTABLE_WIDTH () {
        return TABLE_WIDTH;
    }
    public int getTABLE_HEIGHT () {
        return TABLE_HEIGHT;
    }
    public int getWALL_THICKNESS() {
        return WALL_THICKNESS;
    }
    private boolean isAnyBallMoving() {
        for (Ball ball: BALL_ARRAY) {
            if (ball.isMoving()){
                return true;
            }
        }
        return false;
    }
    private void createInitialBalls(){
        int whiteBallX = ((TABLE_WIDTH + WALL_THICKNESS*2)/2);
        int whiteBallY = 400;
        for (int i = 0; i < NUMBER_OF_BALLS; i++) {
            BALL_ARRAY.add(new Ball(new Coord(0,0), this));
        }
        whiteBall = new WhiteBall(new Coord(whiteBallX, whiteBallY), this);
        BALL_ARRAY.add(whiteBall);
    }
    private void placeBalls(){
        //First row
        double radius = BALL_ARRAY.get(0).getRadius();
        double row1X = 128;
        double row2Y = 100;
        double margin = 3;

        double rad2Y = row2Y + 2*radius-margin;
        double rad3Y = rad2Y + 2*radius-margin;
        double rad4Y = rad3Y + 2*radius-margin;

        //Rad 1
        BALL_ARRAY.getFirst().position = new Coord (row1X, row2Y);
        BALL_ARRAY.get(1).position = new Coord (row1X + 2*radius, row2Y);
        BALL_ARRAY.get(2).position = new Coord (row1X + 4*radius, row2Y);
        BALL_ARRAY.get(3).position = new Coord (row1X + 6*radius, row2Y);

        //Rad 2
        BALL_ARRAY.get(4).position = new Coord (row1X + radius, rad2Y);
        BALL_ARRAY.get(5).position = new Coord (row1X + 3*radius, rad2Y);
        BALL_ARRAY.get(6).position = new Coord (row1X + 5*radius, rad2Y);

        //Rad 3
        BALL_ARRAY.get(7).position = new Coord (row1X + 2*radius, rad3Y);
        BALL_ARRAY.get(8).position = new Coord (row1X + 4*radius, rad3Y);

        //Rad 4
        BALL_ARRAY.get(9).position = new Coord (row1X + 3*radius, rad4Y);

    }
    private void createHoles () {
        HOLE_ARRAY.add(new Hole(new Coord(2*WALL_THICKNESS, 2*WALL_THICKNESS), this));
        HOLE_ARRAY.add(new Hole(new Coord(TABLE_WIDTH, 2*WALL_THICKNESS), this));

        HOLE_ARRAY.add(new Hole(new Coord(2*WALL_THICKNESS, TABLE_HEIGHT*0.5), this));
        HOLE_ARRAY.add(new Hole(new Coord(TABLE_WIDTH, TABLE_HEIGHT*0.5), this));

        HOLE_ARRAY.add(new Hole(new Coord(2*WALL_THICKNESS, TABLE_HEIGHT), this));
        HOLE_ARRAY.add(new Hole(new Coord(TABLE_WIDTH, TABLE_HEIGHT), this));
    }
    public void checkBallCollisions() {
        for (int i = 0; i < BALL_ARRAY.size(); i++){
            for (int j = i + 1; j < BALL_ARRAY.size(); j++){
                Ball ballOne = BALL_ARRAY.get(i);
                Ball ballTwo = BALL_ARRAY.get(j);

                if (ballOne.isCollidingWithBall(ballTwo)) {
                    ballOne.handleBallCollision(ballTwo);
                }
            }
        }
    }
    public void setWhiteballHoleStatus(boolean status) {
        whiteInHole = status;
    }
    public void checkHoleCollisions() {
        for (Ball ball : BALL_ARRAY){
            for (Hole hole: HOLE_ARRAY){
                if (Coord.distance(ball.position, hole.position) < Math.abs(ball.getRadius()-hole.getRadius())){
                    ball.handleHoleEvent(ball);

                    if (ball instanceof WhiteBall) {
                        whiteInHole = true;
                        ball.velocity = new Coord (0,0);
                    }
                }
            }
        }
        updateScore();
    }
    public void removeBallsOutOfPlay() {
        BALL_ARRAY.removeIf(ball -> !ball.isInPlay);
    }
    public boolean validBallPos(Coord mousePosition) {
        for (Ball ball: BALL_ARRAY) {
            if (ball != whiteBall && ball.isInPlay) {
                if (Coord.distance(mousePosition, ball.position) < ball.getRadius() + whiteBall.getRadius()){
                    return false;
                }
            }
        }
        for (Hole hole: HOLE_ARRAY){
            if (Coord.distance(mousePosition, hole.position) < hole.getRadius() + whiteBall.getRadius()){
                return false;
            }
        }
        return true;
    }
    // Obligatory empty listener methods
    public void mousePressed(MouseEvent event) {
        Coord mousePosition = new Coord(event);
        if (whiteInHole) {
            if (validBallPos(mousePosition)){
                whiteBall.position = mousePosition;
                whiteInHole = false;
                placedWhiteBallThisTurn = true;
                repaint();
            }
            return;
        }

        if (Coord.distance(whiteBall.position, mousePosition) < whiteBall.getRadius()){
            if (!isAnyBallMoving()){
                whiteBall.setAimPosition(mousePosition);
            }
        }
        repaint();
    }
    public void mouseReleased(MouseEvent e) {
        if (whiteInHole){
            return;
        }

        if (placedWhiteBallThisTurn) {
            placedWhiteBallThisTurn = false;
            return;
        }

        whiteBall.shoot();
        isShooting = true;

        if (!simulationTimer.isRunning()) {
            simulationTimer.start();
        }
    }
    public void mouseDragged(MouseEvent event) {
        Coord mousePosition = new Coord(event);
        for (Ball ball: BALL_ARRAY) {
            ball.updateAimPosition(mousePosition);
        }
        if (whiteInHole) {
            whiteBall.position = mousePosition;
        }
        repaint();
    }
    public void mouseMoved(MouseEvent event) {
        Coord mousePosition = new Coord(event);
        if (whiteInHole) {
            whiteBall.position = mousePosition;
        }
    }
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    
    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2D = (Graphics2D) graphics;
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // This makes the graphics smoother
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2D.setColor(WALL_COLOR);
        g2D.fillRect(0, 0, TABLE_WIDTH + 2 * WALL_THICKNESS, TABLE_HEIGHT + 2 * WALL_THICKNESS);

        g2D.setColor(COLOR);
        g2D.fillRect(WALL_THICKNESS, WALL_THICKNESS, TABLE_WIDTH, TABLE_HEIGHT);

        for (Hole hole : HOLE_ARRAY) {
            hole.paint(g2D);
        }
        for (Ball ball : BALL_ARRAY) {
            if (ball.isInPlay) {
                ball.paint(g2D);
            }
        }
    }
}  // end class Table

/**
 * ****************************************************************************************
 * Ball:
 *
 * The ball has instance variables relating to its graphics and game state:
 * position, velocity, and the position from which a shot is aimed (if any).
 * 
 */
class Ball {

    protected Color  COLOR               = Color.DARK_GRAY;
    private final int    BORDER_THICKNESS    = 2;
    private final double RADIUS              = 15;
    private final double DIAMETER            = 2 * RADIUS;
    private final double FRICTION            = 0.015;                          // its friction constant (normed for 100 updates/second)
    private final double FRICTION_PER_UPDATE =                                 // friction applied each simulation step
                          1.0 - Math.pow(1.0 - FRICTION,                       // don't ask - I no longer remember how I got to this
                                         100.0 / Twoballs.UPDATE_FREQUENCY);


    public Coord position;
    protected Coord velocity;
    protected Table theTable;
    private Coord aimPosition;
    public boolean isInPlay = true;


    Ball(Coord initialPosition, Table table) {
        position = initialPosition;
        velocity = new Coord (0,0);
        theTable = table;
    }
    public void handleHoleEvent(Ball ball) {
        if (theTable.isPlayer1Turn) {
            theTable.player1Score ++;
        }
        else {
            theTable.player2Score ++;
        }
        ball.isInPlay = false;
        theTable.ABallWasPocketedThisTurn = true;
        theTable.updateScore();
    }
    public void handleWallHit() {
        double leftWall   = theTable.getWALL_THICKNESS();
        double rightWall  = theTable.getWALL_THICKNESS() + theTable.getTABLE_WIDTH();
        double topWall    = theTable.getWALL_THICKNESS();
        double bottomWall = theTable.getTABLE_HEIGHT() + theTable.getWALL_THICKNESS();

        if (position.x + RADIUS >= rightWall && velocity.x > 0){
            bounceHorizontal();
        }

        if (position.x - RADIUS <= leftWall && velocity.x < 0){
            bounceHorizontal();
        }

        if (position.y - RADIUS <= topWall && velocity.y < 0){
            bounceVertical();
        }

        if (position.y + RADIUS >= bottomWall && velocity.y > 0){
            bounceVertical();
        }
    }
    private void bounceVertical() {
        velocity.y = -velocity.y;
    }
    private void bounceHorizontal() {
        velocity.x = -velocity.x;
    }
    public boolean isCollidingWithBall(Ball theOtherBall) {
        double distanceBetweenBalls = Coord.distance(position, theOtherBall.position);
        Coord relativeV = Coord.sub(velocity, theOtherBall.velocity);
        Coord relD = Coord.sub(position, theOtherBall.position);
        double scalar = Coord.scal(relativeV, relD);

        if (distanceBetweenBalls <= RADIUS + theOtherBall.RADIUS && scalar < 0){
            return true;
        }
        return false;
    }
    private void adjustBalls(Ball theOtherBall) {
        //Hanterar buggen med att bollarna fastnar, puttar ut bollarna till deras radie + en liten marginal

        Coord normedVector = position.normBetweenTwoCoords(position, theOtherBall.position);
        double distBetweenBalls = Coord.distance(position, theOtherBall.position);

        double margin = 0.01;
        double adjustLength = (RADIUS + theOtherBall.RADIUS - distBetweenBalls + margin)/2;

        Coord adjustment = Coord.mul(adjustLength, normedVector);

        position.increase(adjustment);
        theOtherBall.position.decrease(adjustment);
    }
    public void handleBallCollision(Ball theOtherBall){
        Coord normedVector = position.normBetweenTwoCoords(position, theOtherBall.position);

        //Hanterar buggen där bollarna åker in i varandra
        adjustBalls(theOtherBall);

        //Enligt mekanikformeln
        double impuls = theOtherBall.velocity.x * normedVector.x + theOtherBall.velocity.y * normedVector.y
                - (velocity.x * normedVector.x + velocity.y * normedVector.y);

        Coord impulsVector = Coord.mul(impuls, normedVector);

        velocity.increase(impulsVector);
        theOtherBall.velocity.decrease(impulsVector);

    }
    public double getRadius(){return RADIUS;}
    boolean isMoving() {    // if moving too slow I am deemed to have stopped
        return velocity.magnitude() > FRICTION_PER_UPDATE;
    }
    void setAimPosition(Coord grabPosition) {
        if (Coord.distance(position, grabPosition) <= RADIUS) {
            aimPosition = grabPosition;
        }
    }

    void updateAimPosition(Coord newPosition) {
        if (isAiming()){
            aimPosition = newPosition;
        }
    }

    void shoot() {
        if (isAiming()) {
            Coord aimingVector = Coord.sub(position, aimPosition);
            velocity = Coord.mul(Math.sqrt(10.0 * aimingVector.magnitude() / Twoballs.UPDATE_FREQUENCY),
                    aimingVector.norm());  // don't ask - determined by experimentation
            aimPosition = null;
        }
    }
    void move() {
         if (isMoving()) {
             position.increase(velocity);
             velocity.decrease(Coord.mul(FRICTION_PER_UPDATE, velocity.norm()));
             handleWallHit();
         }
    }
    private boolean isAiming() {
        return aimPosition != null;
    }
    
    // paint: to draw the ball, first draw a black ball
    // and then a smaller ball of proper color inside
    // this gives a nice thick border
    void paint(Graphics2D g2D) {
        g2D.setColor(Color.black);
        g2D.fillOval(
                (int) (position.x - RADIUS + 0.5),
                (int) (position.y - RADIUS + 0.5),
                (int) DIAMETER,
                (int) DIAMETER);
        g2D.setColor(COLOR);
        g2D.fillOval(
                (int) (position.x - RADIUS + 0.5 + BORDER_THICKNESS),
                (int) (position.y - RADIUS + 0.5 + BORDER_THICKNESS),
                (int) (DIAMETER - 2 * BORDER_THICKNESS),
                (int) (DIAMETER - 2 * BORDER_THICKNESS));
        if (isAiming()) {
            paintAimingLine(g2D);
        }
    }
    private void paintAimingLine(Graphics2D graph2D) {
        Coord.paintLine(
                graph2D,
                aimPosition,
                Coord.sub(Coord.mul(2, position), aimPosition)
        );
    }

}
class WhiteBall extends Ball {
    private final Color  WHITE_COLOR               = Color.white;

    WhiteBall(Coord initialPosition, Table table) {
        super(initialPosition, table);
        this.COLOR = WHITE_COLOR;
    }

    @Override
    public void handleHoleEvent(Ball ball){
        this.velocity = new Coord(0,0);
        theTable.setWhiteballHoleStatus(true);
        theTable.updateScore();
    }

}

class Hole {
    public Coord position;
    private Table theTable;
    protected Color  COLOR               = Color.PINK;
    private final int    BORDER_THICKNESS    = 1;
    private final double RADIUS              = 20;
    private final double DIAMETER            = 2 * RADIUS;

    Hole(Coord initialPosition, Table table) {
        position = initialPosition;
        theTable = table;
    }
    public double getRadius(){return RADIUS;}
    void paint(Graphics2D g2D) {
        g2D.setColor(Color.black);
        g2D.fillOval(
                (int) (position.x - RADIUS + 0.5),
                (int) (position.y - RADIUS + 0.5),
                (int) DIAMETER,
                (int) DIAMETER);
        g2D.setColor(COLOR);
        g2D.fillOval(
                (int) (position.x - RADIUS + 0.5 + BORDER_THICKNESS),
                (int) (position.y - RADIUS + 0.5 + BORDER_THICKNESS),
                (int) (DIAMETER - 2 * BORDER_THICKNESS),
                (int) (DIAMETER - 2 * BORDER_THICKNESS));
    }
}
