import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author Joachim Parrow 2010 rev 2011, 2012, 2013, 2015, 2016
 *
 * Simulator for two balls
 */


public class Twoballs {

    final static int UPDATE_FREQUENCY = 100;    // Global constant: fps, ie times per second to simulate

    public static void main(String[] args) {

        JFrame frame = new JFrame("No collisions!");          
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Table table = new Table();           

        frame.add(table);  
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
    private       Ball  ball1, ball2;
    private final Timer simulationTimer;

    
    Table() {
        
        setPreferredSize(new Dimension(TABLE_WIDTH + 2 * WALL_THICKNESS,
                                       TABLE_HEIGHT + 2 * WALL_THICKNESS));
        createInitialBalls();
        
        addMouseListener(this);
        addMouseMotionListener(this);

        simulationTimer = new Timer((int) (1000.0 / Twoballs.UPDATE_FREQUENCY), this);
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
        final Coord firstInitialPosition = new Coord(100, 100);
        final Coord secondInitialPosition = new Coord(200, 200);
        ball1 = new Ball(firstInitialPosition, this);
        ball2 = new Ball(secondInitialPosition, this);
    }
    
    public void actionPerformed(ActionEvent e) {          // Timer event
        ball1.move(ball2);
        ball2.move(ball1);
        repaint();
        if (!ball1.isMoving() && !ball2.isMoving()) {
            simulationTimer.stop();
        }
    }

    public void mousePressed(MouseEvent event) {
        Coord mousePosition = new Coord(event);
        ball1.setAimPosition(mousePosition);
        ball2.setAimPosition(mousePosition);
        repaint();                                          //  To show aiming line
    }

    public void mouseReleased(MouseEvent e) {
        ball1.shoot();
        ball2.shoot();
        if (!simulationTimer.isRunning()) {
            simulationTimer.start();      
        }
    }

    public void mouseDragged(MouseEvent event) {
        Coord mousePosition = new Coord(event);
        ball1.updateAimPosition(mousePosition);
        ball2.updateAimPosition(mousePosition);
        repaint();
    }

    // Obligatory empty listener methods
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

        ball1.paint(g2D);
        ball2.paint(g2D);
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
    private final int    BORDER_THICKNESS    = 2;
    private final double RADIUS              = 15;
    private final double DIAMETER            = 2 * RADIUS;
    private final double FRICTION            = 0.015;                          // its friction constant (normed for 100 updates/second)
    private final double FRICTION_PER_UPDATE =                                 // friction applied each simulation step
                          1.0 - Math.pow(1.0 - FRICTION,                       // don't ask - I no longer remember how I got to this
                                         100.0 / Twoballs.UPDATE_FREQUENCY);           
    private Coord position;
    private Coord velocity;
    private Coord aimPosition;              // if aiming for a shot, ow null
    private Table theTable;

    Ball(Coord initialPosition, Table table) {
        position = initialPosition;
        velocity = Coord.ZERO;
        theTable = table;
    }

    private void handleWallHit() {
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
    private boolean isCollidingWithBall(Ball theOtherBall) {
        double distanceBetweenBalls;
        double yDiff = position.y - theOtherBall.position.y;
        double xDiff = position.x - theOtherBall.position.x;

        distanceBetweenBalls = Math.sqrt(yDiff*yDiff + xDiff*xDiff);
        if (distanceBetweenBalls <= RADIUS + theOtherBall.RADIUS){
            System.out.println("krock!");
            return true;
        }
        return false;
    }
    private void handleBallCollision(Ball theOtherBall){

    }
    private void stopBall(){
        velocity.x = 0;
        velocity.y = 0;
    }
    private boolean isAiming() {
        return aimPosition != null;
    }
    
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
    
    void move(Ball theOtherBall) {
         if (isMoving()) {
             position.increase(velocity);
             velocity.decrease(Coord.mul(FRICTION_PER_UPDATE, velocity.norm()));
             handleWallHit();
         }
         if (isCollidingWithBall(theOtherBall)) {
             handleBallCollision(theOtherBall);
             stopBall();
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
} // end  class Ball  
