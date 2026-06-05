package com.eric.javablock;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;

public class MethodCallBlock extends StackPane {

    private TextField objectNameInput;
    private TextField methodNameInput;
    private TextField methodArgsInput;

    private double dragStartX;
    private double dragStartY;
    private List<javafx.scene.Node> tailBlocks = new ArrayList<>();

    public MethodCallBlock() {
        // 物件操作系列，使用亮橘紅色系 (ORANGERED)
        Rectangle rect = new Rectangle(320, 40, Color.ORANGERED);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        HBox content = new HBox(5);
        content.setAlignment(Pos.CENTER);

        // 物件名稱輸入框 (例如: dog)
        objectNameInput = new TextField("dog");
        objectNameInput.setPrefWidth(60);

        Text t1 = new Text(".");
        t1.setFill(Color.WHITE);
        t1.setFont(Font.font("System", 14));

        // 方法名稱輸入框 (例如: bark)
        methodNameInput = new TextField("bark");
        methodNameInput.setPrefWidth(70);

        Text t2 = new Text("(");
        t2.setFill(Color.WHITE);
        t2.setFont(Font.font("System", 14));

        // 呼叫方法時傳入的參數框 (預設留白)
        methodArgsInput = new TextField("");
        methodArgsInput.setPromptText("參數");
        methodArgsInput.setPrefWidth(60);

        Text t3 = new Text(");");
        t3.setFill(Color.WHITE);
        t3.setFont(Font.font("System", 14));

        content.getChildren().addAll(objectNameInput, t1, methodNameInput, t2, methodArgsInput, t3);
        this.getChildren().addAll(rect, content);

        // --- 在工作區重複拖拉與串接尾巴的滑鼠事件 ---
        this.setOnMousePressed(event -> {
            dragStartX = event.getSceneX() - this.getLayoutX();
            dragStartY = event.getSceneY() - this.getLayoutY();

            tailBlocks.clear();
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();

            if (parent != null) {
                double currentY = this.getLayoutY();
                double currentHeight = this.getBoundsInParent().getHeight();
                boolean found = true;

                while (found) {
                    found = false;
                    for (javafx.scene.Node node : parent.getChildren()) {
                        if (node != this && !tailBlocks.contains(node)) {
                            if (Math.abs(node.getLayoutX() - this.getLayoutX()) < 5 &&
                                    Math.abs(node.getLayoutY() - (currentY + currentHeight)) < 5) {

                                tailBlocks.add(node);
                                currentY = node.getLayoutY();
                                currentHeight = node.getBoundsInParent().getHeight();
                                found = true;
                                break;
                            }
                        }
                    }
                }
                this.toFront();
                for (javafx.scene.Node tail : tailBlocks) {
                    tail.toFront();
                }
            }
        });

        this.setOnMouseDragged(event -> {
            double newX = event.getSceneX() - dragStartX;
            double newY = event.getSceneY() - dragStartY;

            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();
            if (parent != null) {
                double blockWidth = this.getBoundsInParent().getWidth();
                double blockHeight = this.getBoundsInParent().getHeight();
                if (newX < 0) newX = 0;
                if (newY < 0) newY = 0;
                if (newX > parent.getWidth() - blockWidth) newX = parent.getWidth() - blockWidth;
                if (newY > parent.getHeight() - blockHeight) newY = parent.getHeight() - blockHeight;
            }

            double deltaX = newX - this.getLayoutX();
            double deltaY = newY - this.getLayoutY();

            this.setLayoutX(newX);
            this.setLayoutY(newY);

            for (javafx.scene.Node tail : tailBlocks) {
                tail.setLayoutX(tail.getLayoutX() + deltaX);
                tail.setLayoutY(tail.getLayoutY() + deltaY);
            }
        });
    }

    public String getJavaCode() {
        return objectNameInput.getText() + "." + methodNameInput.getText() + "(" + methodArgsInput.getText() + ");";
    }
}