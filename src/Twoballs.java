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

        JButton button = new JButton("Pausa");
        Table table = new Table();

        frame.add(table, BorderLayout.CENTER);
        frame.add(button, BorderLayout.EAST);

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

    static Coord giveRandomVector (double upperLimit){
        double signFlip = -1;
        double randomX = upperLimit * Math.random();
        double randomY = upperLimit * Math.random();
        if (Math.random() < 0.5){ randomX = randomX * signFlip;}
        if (Math.random() < 0.5){ randomY = randomY * signFlip;}

        return new Coord (randomX, randomY);
    }

    static Coord giveRandomStartPos(double width, double height, double wallThickness) {
        double randomX = wallThickness + Math.random() * width/2;
        double randomY = wallThickness + Math.random() * height;

        return new Coord (randomX, randomY);
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

    private final int   TABLE_WIDTH    = 700;
    private final int   TABLE_HEIGHT   = 700;
    private final int   WALL_THICKNESS = 20;
    private final Color COLOR          = Color.green;
    private final Color WALL_COLOR     = Color.black;
    private final Timer simulationTimer;
    private final int   NUMBER_OF_BALLS = 70;
    private final int   NUMBER_OF_SICK_BALLS = 5;
    private final List<Ball>  BALL_ARRAY = new ArrayList<>();
    
    Table() {
        
        setPreferredSize(new Dimension(TABLE_WIDTH + 2 * WALL_THICKNESS,
                                       TABLE_HEIGHT + 2 * WALL_THICKNESS));
        createInitialBalls();
        
        addMouseListener(this);
        addMouseMotionListener(this);

        simulationTimer = new Timer((int) (1000.0 / Twoballs.UPDATE_FREQUENCY), this);
        simulationTimer.start();
    }
    public void actionPerformed(ActionEvent e) {          // Timer event
        for (Ball ball: BALL_ARRAY){
            ball.move();
        }
        checkCollision();
        removeDeadBalls(BALL_ARRAY);
        repaint();
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
    private void createInitialBalls(){

        for (int i = 0; i < NUMBER_OF_BALLS; i++) {
            Coord randomStartPos = Coord.giveRandomStartPos(TABLE_WIDTH, TABLE_HEIGHT, WALL_THICKNESS);
            BALL_ARRAY.add(new Ball(randomStartPos, this));
        }
        createInitSickBalls(BALL_ARRAY);
    }
    private void createInitSickBalls (List<Ball> ballArray) {
        for (int i = 0; i < NUMBER_OF_SICK_BALLS; i++) {
            ballArray.get(i).isSick = true;
        }
    }
    public void removeDeadBalls(List<Ball> ballArray) {
        ballArray.removeIf(ball -> ball.isDead);
    }
    public void checkCollision() {
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
    public int countDead() {
        int numberOfDead = 0;
        for (Ball ball: BALL_ARRAY) {
            if (ball.isDead) {
                numberOfDead ++;
            }
        }
        return numberOfDead;
    }
    public int countSick() {
        int numberOfSick = 0;
        for (Ball ball: BALL_ARRAY) {
            if (ball.isSick) {
                numberOfSick ++;
            }
        }
        return numberOfSick;
    }
    // Obligatory empty listener methods
    public void mousePressed(MouseEvent event) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseDragged(MouseEvent event) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}
    
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

        for (Ball ball : BALL_ARRAY) {
            ball.paint(g2D);
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

    private final Color  COLOR               = Color.white;
    private final Color  SICK_COLOR          = Color.red;
    private final int    BORDER_THICKNESS    = 2;
    private final double RADIUS              = 8;
    private final double DIAMETER            = 2 * RADIUS;
    private final double FRICTION            = 0;                          // its friction constant (normed for 100 updates/second)
    private final double FRICTION_PER_UPDATE =                                 // friction applied each simulation step
                          1.0 - Math.pow(1.0 - FRICTION,                       // don't ask - I no longer remember how I got to this
                                         100.0 / Twoballs.UPDATE_FREQUENCY);
    private final double upperVelocityLimit = 2;
    private final double INFECT_PROB = 0.10;
    private final double DIE_PROB = 0.05;
    private final double HEAL_PROB = 0.1;
    private final double HEAL_OR_DIE_TIMER = 3;

    private Coord position;
    private Coord velocity;
    private Table theTable;
    private Coord startVelocity;
    public boolean isSick = false;
    public boolean isDead = false;
    public int tickCounterSinceInfected = 0;


    Ball(Coord initialPosition, Table table) {
        startVelocity = Coord.giveRandomVector(upperVelocityLimit);

        position = initialPosition;
        velocity = startVelocity;
        theTable = table;
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

        //Försök smitta
        attemptInfect(theOtherBall);

        //Enligt mekanikformeln
        double impuls = theOtherBall.velocity.x * normedVector.x + theOtherBall.velocity.y * normedVector.y
                - (velocity.x * normedVector.x + velocity.y * normedVector.y);

        Coord impulsVector = Coord.mul(impuls, normedVector);

        velocity.increase(impulsVector);
        theOtherBall.velocity.decrease(impulsVector);

    }
    public void attemptInfect(Ball theOtherball) {
        if (this.isSick && !theOtherball.isSick && Math.random() < INFECT_PROB) {
            theOtherball.isSick = true;
        }
        else if (theOtherball.isSick && !this.isSick && Math.random() < INFECT_PROB){
            this.isSick = true;
        }
    }

    boolean isMoving() {    // if moving too slow I am deemed to have stopped
        return velocity.magnitude() > FRICTION_PER_UPDATE;
    }
    private void killOrHeal () {
        double randomNumber = Math.random();
        if (randomNumber <= HEAL_PROB){
            isSick = false;
            tickCounterSinceInfected = 0;
            System.out.println("någon blev frisk");
        }
        else if (randomNumber <= HEAL_PROB + DIE_PROB){
            isDead = true;
            tickCounterSinceInfected = 0;
            System.out.println("någon dog");
        }
        else {
            tickCounterSinceInfected = 0;
        }
    }
    void move() {
         if (isMoving()) {
             position.increase(velocity);
             velocity.decrease(Coord.mul(FRICTION_PER_UPDATE, velocity.norm()));
             handleWallHit();
         }
         if (isSick) {
             tickCounterSinceInfected ++;

             if (tickCounterSinceInfected == HEAL_OR_DIE_TIMER * Twoballs.UPDATE_FREQUENCY) {
                 killOrHeal();
             }
         }
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
        if (isSick) {
            g2D.setColor(SICK_COLOR);
        }
        else {
            g2D.setColor(COLOR);
        }
        g2D.fillOval(
                (int) (position.x - RADIUS + 0.5 + BORDER_THICKNESS),
                (int) (position.y - RADIUS + 0.5 + BORDER_THICKNESS),
                (int) (DIAMETER - 2 * BORDER_THICKNESS),
                (int) (DIAMETER - 2 * BORDER_THICKNESS));
    }

} // end  class Ball  
