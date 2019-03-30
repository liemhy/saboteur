package gui;

import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Board;
import model.GoalType;
import model.Position;
import model.cards.Card;

import java.util.HashMap;
import java.util.Map;

public class BoardPane extends Pane {
  private GameGUIController controller;

  private Map<Position, CardPane> cardPanes;

  private CardPane topGoal;
  private CardPane middleGoal;
  private CardPane bottomGoal;

  private double lastX, lastY;
  private boolean dragged;

  public BoardPane(GameGUIController controller) {
    this.controller = controller;
    cardPanes = new HashMap<>();
  }

  void focusAt(int x, int y) {
    focusAt(x, y, controller.getWidth(), controller.getHeight());
  }

  private void focusAt(int x, int y, double screenWidth, double screenHeight) {
    double centerX = screenWidth / 2;
    double centerY = screenHeight / 2;
    Point2D target = getPosRelativeToScreen(x, y);
    TranslateTransition t = new TranslateTransition();
    t.setByX(centerX - target.getX());
    t.setByY(centerY - target.getY());
    t.setInterpolator(Interpolator.EASE_BOTH);
    t.setDuration(Duration.millis(500));
    t.setNode(this);
    t.play();
  }

  void placeCard(Card card, int x, int y, boolean rotated) {
    CardPane cardPane = new CardPane(card);
    cardPane.setLayoutY(y * CardPane.HEIGHT);
    cardPane.setLayoutX(x * CardPane.WIDTH);
    cardPane.setRotationAxis(new Point3D(0, 0, 1));
    if (rotated) cardPane.setRotate(180);
    cardPane.setOpacity(0);
    getChildren().add(cardPane);

    FadeTransition ft = new FadeTransition();
    ft.setInterpolator(Interpolator.EASE_BOTH);
    ft.setDelay(Duration.millis(250));
    ft.setDuration(Duration.millis(450));
    ft.setFromValue(0.0);
    ft.setToValue(1.0);
    ft.setNode(cardPane);
    ft.play();

    cardPanes.put(new Position(x, y), cardPane);
  }

  void destroyCard(int x, int y) {
    CardPane cardPane = cardPanes.get(new Position(x, y));

    FadeTransition ft = new FadeTransition();
    ft.setInterpolator(Interpolator.EASE_BOTH);
    ft.setDelay(Duration.millis(250));
    ft.setDuration(Duration.millis(450));
    ft.setFromValue(1.0);
    ft.setToValue(0.0);
    ft.setNode(cardPane);
    ft.setOnFinished(e -> getChildren().remove(cardPane));
    ft.play();
  }

  private Animation generateToggleGoalCardAnimation(Board.GoalPosition pos, boolean open) {
    GoalType goal = controller.state().getGoal(pos);
    String name = open ? goal == GoalType.GOLD ? "goal_gold" : "goal_rock_1" : "goal_back";
    CardPane opened = new CardPane(name);

    CardPane closed = pos == Board.GoalPosition.TOP ?
      topGoal : pos == Board.GoalPosition.MIDDLE ?
      middleGoal : pos == Board.GoalPosition.BOTTOM ?
      bottomGoal : topGoal;

    System.out.println("closed: " + closed);

    closed.setRotationAxis(new Point3D(0, 1, 0));
    opened.setRotationAxis(new Point3D(0, 1, 0));
    opened.setRotate(90);
    opened.setLayoutX(closed.getLayoutX());
    opened.setLayoutY(closed.getLayoutY());
    System.out.println("added: " + opened);
    getChildren().add(opened);

    RotateTransition closeTransition = new RotateTransition();
    closeTransition.setFromAngle(0);
    closeTransition.setToAngle(90);
    closeTransition.setDuration(Duration.millis(250));
    closeTransition.setOnFinished(e -> {
      System.out.println("removed: " + closed);
      getChildren().remove(closed);
    });

    RotateTransition openTransition = new RotateTransition();
    openTransition.setFromAngle(90);
    openTransition.setToAngle(0);
    openTransition.setDuration(Duration.millis(250));
    openTransition.setOnFinished(e -> {
      System.out.println("set: " + opened);
      switch (pos) {
        case TOP: topGoal = opened; break;
        case MIDDLE: middleGoal = opened; break;
        case BOTTOM: bottomGoal = opened; break;
      }
    });

    closeTransition.setNode(closed);
    openTransition.setNode(opened);

    return new SequentialTransition(closeTransition, openTransition);
  }

  GoalType openGoalCard(Board.GoalPosition pos) {
    Animation open = generateToggleGoalCardAnimation(pos, true);
    open.play();
    return controller.state().getGoal(pos);
  }

  GoalType peekGoalCard(Board.GoalPosition pos) {
    Animation open = generateToggleGoalCardAnimation(pos, true);
    open.setOnFinished(e -> {
      Animation close = generateToggleGoalCardAnimation(pos, false);
      close.setDelay(Duration.millis(500));
      close.play();
    });
    open.play();
    return controller.state().getGoal(pos);
  }

  private void handlePress(MouseEvent event) {
    lastX = event.getScreenX();
    lastY = event.getSceneY();
    dragged = true;
    setCursor(Cursor.CLOSED_HAND);
  }

  private void handleMove(MouseEvent event) {
    if (dragged) {
      setTranslateX(getTranslateX() + event.getScreenX() - lastX);
      setTranslateY(getTranslateY() + event.getScreenY() - lastY);

      lastX = event.getScreenX();
      lastY = event.getScreenY();
    }
  }

  private void handleRelease() {
    setCursor(Cursor.OPEN_HAND);
    System.out.println("release");
    dragged = false;
  }


  private Point2D getPosRelativeToScreen(int x, int y) {
    double translateX = getTranslateX();
    double translateY = getTranslateY();
    double relativeToPaneX = x * CardPane.WIDTH + CardPane.WIDTH / 2;
    double relativeToPaneY = y * CardPane.HEIGHT + CardPane.HEIGHT / 2;
    return new Point2D(translateX + relativeToPaneX, translateY + relativeToPaneY);
  }

  void initialize() {
    CardPane start = new CardPane("start");
    topGoal = new CardPane("goal_back");
    middleGoal = new CardPane("goal_back");
    bottomGoal = new CardPane("goal_back");

    start.setLayoutX(0 * CardPane.WIDTH);
    start.setLayoutY(2 * CardPane.HEIGHT);

    topGoal.setLayoutX(8 * CardPane.WIDTH);
    topGoal.setLayoutY(0 * CardPane.HEIGHT);

    middleGoal.setLayoutX(8 * CardPane.WIDTH);
    middleGoal.setLayoutY(2 * CardPane.HEIGHT);

    bottomGoal.setLayoutX(8 * CardPane.WIDTH);
    bottomGoal.setLayoutY(4 * CardPane.HEIGHT);

    Rectangle border = new Rectangle(9 * CardPane.WIDTH + 8, 5 * CardPane.HEIGHT + 8);
    border.setFill(Color.TRANSPARENT);
    border.setStroke(new Color(1, 1, 1, .5));
    border.setStrokeWidth(2);
    border.setLayoutX(-4);
    border.setLayoutY(-4);

    getChildren().addAll(start, topGoal, middleGoal, bottomGoal, border);


    setOnMousePressed(this::handlePress);
    setOnMouseReleased(event -> handleRelease());
    setOnMouseDragged(this::handleMove);


    setCursor(Cursor.OPEN_HAND);
  }
}