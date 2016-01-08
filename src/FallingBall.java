package fallingball;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class FallingBall extends Application{
    /**
     * author: D-Pow
     * 12-30-15
     */
    
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        MovingBall ballPane = new MovingBall();
        
        HBox buttonBox = new HBox(4);
        Button addButton = new Button("Add Ball");
        addButton.setOnAction((ActionEvent e) -> ballPane.addBall());
        Button physicsButton = new Button("Physics");
        physicsButton.setOnAction((ActionEvent e) -> ballPane.togglePhysics());
        Button resetButton = new Button("Reset");
        resetButton.setOnAction((ActionEvent e) -> ballPane.reset());
        buttonBox.getChildren().addAll(addButton, physicsButton, resetButton);
        
        //Use BorderPane as layout manager
        BorderPane pane = new BorderPane();
        pane.setCenter(ballPane);
        pane.setBottom(buttonBox);
        
        Scene scene = new Scene(pane, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Falling_Ball");
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        
    }
    
}

class MovingBall extends Pane{
    private int sleepSpeed = 10;
    private boolean physics = false;
    
    public MovingBall(){
        Thread thread = new Thread(()->{
            try {
                while (true){
                    Platform.runLater(()->moveBall());
                    TimeUnit.MILLISECONDS.sleep(sleepSpeed);
                }
            }
            catch (InterruptedException ex) {
            } 
        });
        thread.start();
    }
    
    public void addBall(){
        this.getChildren().add(new Ball());
    }
    
    public void reset(){
        this.getChildren().clear();
        this.physics = false;
    }
    
    public void moveBall(){
        for (Node ballNode : this.getChildren()){
            Ball ball = (Ball) ballNode;
            //Since "ball" is a node, I have to cast it as a (Ball) to get
            //the methods in the class Ball
            if (physics){
                //Notice how this is before the checkWallCollision.
                //That's b/c the ball time needs to be reset if the ball
                //hits the bottom wall. If the ball didn't hit the bottom wall,
                //then t needs to be updated (so that the speed is increased)
                //using the line below.
                ball.t = (System.currentTimeMillis() - ball.startTime)/1000;
            }
            checkWallCollision(ball);
            ball.setCenterX(ball.getCenterX() + ball.dx);
            if (physics){
                //Change the ball's velocity "dyPhysics" and change the position accordingly
                ball.dyPhysics = (int) Math.round(9.8*ball.t + ball.yVelocity);
                ball.setCenterY(ball.getCenterY() + ball.dyPhysics);
            }
            else if (!physics){
                ball.setCenterY(ball.getCenterY() + ball.dy);
            }
        }
        checkBallCollision();
    }
    
    public void checkWallCollision(Ball ball){
        //Change the x-direction if the ball hits a wall
        if (ball.getCenterX() + ball.radius > this.getWidth() || ball.getCenterX() - ball.radius < 0){
            ball.dx *= -1;
        }
        
        //Change the y-direction of the ball if it hits a wall
        //Move ball out of wall by the distance (how far it is into the wall)
        //plus an extra pixel for safety
        if (ball.getCenterX() + ball.radius > this.getWidth()){
            int distance = (int) (this.getWidth() - (ball.getCenterX() + ball.radius));
            ball.setCenterX(ball.getCenterX() - 1 + distance);
        }
        else if (ball.getCenterX() - ball.radius < 0){
            int distance = (int) (ball.getCenterX() - ball.radius);
            ball.setCenterX(ball.getCenterX() + 1 - distance);
        }
        if (ball.getCenterY() + ball.radius > this.getHeight()){
            ball.dy = -1; //must be hard-coded because if it isn't, when the ball
                          //falls due to physics, then the dy will already be -1
                          //and hitting the wall again would turn it into +1
                          //if it were typed like this: "ball.dy *= -1;"
            ball.setCenterY(this.getHeight() - ball.radius - 10);
            
            //If physics is enabled, do the following
            if (physics){
                //ball.yVelocity = the speed it was going before bouncing
                //multiplied by a dampening coefficient
                ball.yVelocity = ball.dyPhysics*ball.dy*0.85;
                //reset the ball's t value so it doesn't keep accelerating
                ball.startTime = System.currentTimeMillis();
            }
        }
        else if (ball.getCenterY() - ball.radius < 0){
            ball.dy = 1; //again, this must be hard-coded
            int distance = (int) (ball.getCenterY() - ball.radius);
            ball.setCenterY(ball.getCenterY() + 1 - distance);
        }
    }
    
    public void checkBallCollision(){
        List<Node> list = this.getChildren();
        
        for (int i = 0; i<list.size(); i++){
            for (int j = i+1; j<list.size(); j++){
                Ball a = (Ball) list.get(i);
                Ball b = (Ball) list.get(j);
                if (a.collideWith(b)){
                    int distance = a.radius + b.radius;
                    double actualDistance = Math.sqrt(Math.pow(a.getCenterX()-b.getCenterX(),2)
                            + Math.pow(a.getCenterY()-b.getCenterY(), 2));
                    double collideAmount = distance - actualDistance;
                    
                    //Make sure the two balls bounce in the right direction
                    //and prevent them from getting stuck by moving them a little bit
                    if (a.dx == b.dx){
                        a.dy *= -1;
                        b.dy *= -1;
                        a.setCenterY(a.getCenterY() + Math.round(collideAmount/2 + 1)*a.dy);
                        b.setCenterY(b.getCenterY() + Math.round(collideAmount/2 + 1)*b.dy);
                    }
                    else if (a.dy == b.dy){
                        a.dx *= -1;
                        b.dx *= -1;
                        a.setCenterX(a.getCenterX() + Math.round(collideAmount/2 + 1)*a.dx);
                        b.setCenterX(b.getCenterX() + Math.round(collideAmount/2 + 1)*b.dx);
                    }
                }
            }
        }
    }//end checkBallCollision()
    
    public void togglePhysics(){
        this.physics = !this.physics; //toggle physics on and off
        if (physics){
            for (Node node : getChildren()){
                Ball ball = (Ball) node;
                ball.yVelocity = ball.dy; //this is just to reset the yVelocity of the balls
                ball.startTime = System.currentTimeMillis(); //Need to reset the t value when physics is turned on
            }
        }
    }
}

class Ball extends Circle{
    public int dx = 1;
    public int dy = 1;
    public int radius = 10;
    public double yVelocity;
    public double startTime; //used to find each ball's t (time value)
    public double t; //the ball's time value
    public int dyPhysics; //the amount the ball's y coordinate will change
                           //when physics is active. This becomes the ball's
                           //velocity when it bounces.
    
    public Ball(){
        this(10,10);
    }
    
    public Ball(int x, int y){
        setRadius((double) radius);
        setCenterX(x);
        setCenterY(y);
        startTime = System.currentTimeMillis();
        t = 0;
        dyPhysics = 0;
    }
    
    public boolean collideWith(Ball b){
        double distanceX = getCenterX() - b.getCenterX();
        double distanceY = getCenterY() - b.getCenterY();
        double distance = Math.sqrt(Math.pow(distanceX,2) + Math.pow(distanceY, 2));
        
        return (distance < (b.getRadius() + getRadius()));
    }
}
